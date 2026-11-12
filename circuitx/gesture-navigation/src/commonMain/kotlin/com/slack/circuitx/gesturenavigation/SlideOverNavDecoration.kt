// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.determineAnimatedNavEvent
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation

// Fraction of the screen that the background screen is offset during transitions.
private const val EnterOffsetFraction = 0.25f
// Duration for child animations. Uses LinearEasing so a gesture's seekTo() fraction maps 1:1 to
// the visual offset, keeping the screen under the finger.
private const val TransitionDurationMs = 300
// Maximum alpha for the scrim overlay (0.32 = 32% opacity black)
private const val ScrimMaxAlpha = 0.32f
private val TransitionSpec = tween<Float>(TransitionDurationMs, easing = LinearEasing)

// Programmatic navigation drives the transition fraction with its own spec, so it sets the felt
// duration and curve while the children stay linear for gestures. Matches the vintage ViewPager,
// which settles a full page in 200ms on a quintic ease-out: leaves fast, decelerates into the stop.
private const val NavigationDurationMs = 200
private val NavigationSpec = tween<Float>(NavigationDurationMs, easing = EaseOutQuint)

// Slot id for the scrim in the record SubcomposeLayout.
private object ScrimSlotId

/**
 * iOS-style slide-over navigation decorator. Hosts every record in one shared [SubcomposeLayout]
 * and warms adjacent screens off screen so swipes stay smooth.
 *
 * The top screen slides in over a dimmed background screen; a root reset crossfades instead, since
 * it has no adjacent to slide from. All visual effects (slide offset, fade alpha, scrim) are driven
 * by a single [SeekableTransitionState]: gestures seek it directly via an [AnchoredDraggableState],
 * while programmatic navigation animates it.
 *
 * @param canGoForward whether a forward swipe is allowed for the given stack. Defaults to true when
 *   forward history exists.
 * @param canGoBackward whether a backward swipe is allowed for the given stack. Defaults to true
 *   when backward history exists.
 * @param swipeEnabled whether the horizontal swipe is enabled right now, read in composition. Use
 *   it to pause swiping while content needs horizontal drags, such as while the IME is visible.
 *   Doesn't affect system back. Defaults to always enabled.
 * @param preloadEnabled whether adjacent screens are warmed (precomposed off screen) ahead of a
 *   gesture. Defaults to false, since warming costs an off-screen composition per adjacent.
 * @param onWarmEvent observes warm and settle events. Defaults to a no-op. Invoked on the main
 *   thread.
 */
public class SlideOverNavDecoration(
  private val canGoForward: (NavStackList<out NavArgument>) -> Boolean = { it.forwardItems.any() },
  private val canGoBackward: (NavStackList<out NavArgument>) -> Boolean = {
    it.backwardItems.any()
  },
  private val swipeEnabled: @Composable () -> Boolean = { true },
  private val preloadEnabled: Boolean = false,
  private val onWarmEvent: (WarmEvent) -> Unit = {},
) : NavDecoration {

  /**
   * Observability for adjacent-screen warming. Lets a test or telemetry see when warming fires and
   * whether a navigation landed on a pre-warmed record.
   */
  public sealed interface WarmEvent {
    /** A record's off-screen precomposition finished and is now adoptable, keyed by record key. */
    public data class Warmed(val key: String) : WarmEvent

    /**
     * A record became the settled active screen. [warmHit] is true when that record had been warmed
     * as an adjacent before this navigation, i.e. the swipe was served from a warm node.
     */
    public data class Settled(val key: String, val warmHit: Boolean) : WarmEvent

    /**
     * Off-screen precomposition of the record failed with [cause] and was swallowed rather than
     * crashing the visible screen. A failure here usually means the `_registry_` invariant
     * regressed on the warm path.
     */
    public data class Failed(val key: String, val cause: Throwable) : WarmEvent
  }

  @OptIn(InternalCircuitApi::class) // For PredictiveBackEventHandler and determineAnimatedNavEvent
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    @Suppress("SlotReused") content: @Composable ((T) -> Unit),
  ) {
    // Transition and drag states
    val seekableTransitionState = remember { SeekableTransitionState(args) }
    val transition = rememberTransition(seekableTransitionState, label = "SlideOverNavDecoration")
    val anchoredState = remember(args) { AnchoredDraggableState(SlideOverAnchor.Center) }
    val settleScope = rememberCoroutineScope()

    // NavStack states
    val canGoBackward = remember(args) { canGoBackward(args) }
    val canGoForward = remember(args) { canGoForward(args) }
    val keepMounted = remember(args) { keepMountedKeys(args) }
    val recordLayoutState = rememberRecordLayoutState(keepMounted)
    val previousNavStackList = rememberPrevious(args)
    val nextNavStackList = rememberNext(args)

    val animatedNavEvent =
      remember(transition.currentState, transition.targetState) {
        determineAnimatedNavEvent(transition.currentState, transition.targetState)
      }

    // Reset for the current active screen
    LaunchedEffect(args) {
      seekableTransitionState.animateTo(args, NavigationSpec)
    }

    LaunchedEffect(args) {
      snapshotFlow { anchoredState.settledValue }
        .collect { anchor ->
          when (anchor) {
            SlideOverAnchor.Start -> navigator.forward()
            SlideOverAnchor.Center -> Unit
            SlideOverAnchor.End -> navigator.backward()
          }
        }
    }

    SeekOnDrag(
      seekableTransitionState,
      anchoredState,
      previousNavStackList,
      SlideOverAnchor.End,
      args,
    )
    SeekOnDrag(
      seekableTransitionState,
      anchoredState,
      nextNavStackList,
      SlideOverAnchor.Start,
      args,
    )

    // A lambda, not a captured Boolean: PreloadAdjacentRecord feeds this to snapshotFlow, which
    // re-emits only when snapshot state read inside the block changes. Reading the state here (not
    // a precomputed value) lets the flow wake when the transition settles.
    val settledProvider = {
      !anchoredState.isAnimationRunning &&
        args == seekableTransitionState.currentState &&
        args == seekableTransitionState.targetState &&
        !transition.isRunning
    }

    // Keys whose paced precomposition has been applied and is adoptable. The record layout places
    // only these off-screen, so it never subcomposes a not-yet-warmed record cold in one pass.
    // Snapshot-backed so the async preload's write recomposes the synchronous reader below;
    // PreloadAdjacentRecord's finally removes a stale key.
    val warmReady = remember { mutableStateSetOf<String>() }
    val onWarmReady: (T, Boolean) -> Unit = { record, ready ->
      if (ready) warmReady.add(record.key) else warmReady.remove(record.key)
      if (ready) onWarmEvent(WarmEvent.Warmed(record.key))
    }

    // One RecordHost per record key, shared by the warm precompose, the on-screen slide/settled
    // slots, and the placement below. Every role subcomposes the key with the host's single
    // slotContent instance, so navigation adopts the warmed node instead of recomposing it.
    val hosts = remember { mutableMapOf<String, RecordHost<T>>() }
    val hostFor: (T) -> RecordHost<T> = { record ->
      hosts.getOrPut(record.key) { RecordHost(record, content) }
    }

    val backwardAdjacent = previousNavStackList?.active
    val forwardAdjacent = nextNavStackList?.active
    // Precompose adjacents into the shared record layout so navigation adopts the warmed node. Flag
    // off keeps warmReady empty, so no warm/bridge slots are placed.
    WarmAdjacentRecords(
      backwardAdjacent = backwardAdjacent.takeIf { canGoBackward && preloadEnabled },
      forwardAdjacent = forwardAdjacent.takeIf { canGoForward && preloadEnabled },
      settledProvider = settledProvider,
      layoutState = recordLayoutState,
      hostFor = hostFor,
      onWarmReady = onWarmReady,
      onWarmFailed = { record, cause -> onWarmEvent(WarmEvent.Failed(record.key, cause)) },
    )

    // Warmed adjacents folded into the mounted set, so they are placed off screen and their interop
    // views attach ahead of navigation. Gated on canGo* and applied, not hasSettled, so the warmed
    // key stays subcomposed across the settle-to-slide boundary. Dropping it mid-swipe disposes its
    // node, and the slide then re-subcomposes the key cold, re-registering its `_registry_` before
    // the disposal drains.
    // args.active bridges the programmatic-nav handoff: `args` commits the destination one pass
    // before the transition follows, so that pass still renders `settled` on the old record.
    // Hosting the committed record off screen keeps its warmed node subcomposed across that gap.
    val warmAdjacents =
      listOfNotNull(
          backwardAdjacent?.takeIf { canGoBackward && it.key in warmReady },
          forwardAdjacent?.takeIf { canGoForward && it.key in warmReady },
          args.active.takeIf { preloadEnabled },
        )
        .distinctBy { it.key }
        .map { record -> WarmAdjacent(record, hostFor(record)) }

    SettleBookkeeping(hasSettled = settledProvider(), args = args, warmReady = warmReady) {
      active,
      warmHit ->
      hosts.retainKeys(keepMounted)
      onWarmEvent(WarmEvent.Settled(active.key, warmHit))
    }

    val swipeEnabled = swipeEnabled()
    Box(
      modifier =
        modifier
          .directionalAnchoredDrag(
            state = anchoredState,
            enabled = swipeEnabled && (canGoBackward || canGoForward),
            settleScope = settleScope,
          )
          .onSizeChanged { intSize ->
            anchoredState.updateAnchors(intSize, canGoForward, canGoBackward)
          }
    ) {
      transition.Content(
        event = animatedNavEvent,
        layoutState = recordLayoutState,
        warmAdjacents = warmAdjacents,
        keepMountedKeys = keepMounted,
        canPruneMountedRecords = settledProvider(),
        hostFor = hostFor,
      )
    }
    // Use predictive back handler for system backward gestures
    PredictiveBackEventHandler(
      isEnabled = canGoBackward,
      onBackProgress = { progress, _ ->
        seekableTransitionState.seekToNav(progress, previousNavStackList)
      },
      onBackCancelled = {
        seekableTransitionState.resetNav(args)
      },
      onBackCompleted = {
        navigator.pop()
      },
    )
  }
}

/**
 * Seeks [transitionState] toward [targetNavStack] as the drag progresses from center to [edge],
 * resetting home on the terminal frame. Keyed on [targetNavStack] so a new adjacent restarts it.
 */
@Composable
private fun <T : NavArgument> SeekOnDrag(
  transitionState: SeekableTransitionState<NavStackList<T>>,
  anchoredState: AnchoredDraggableState<SlideOverAnchor>,
  targetNavStack: NavStackList<T>?,
  edge: SlideOverAnchor,
  args: NavStackList<T>,
) {
  LaunchedEffect(targetNavStack) {
    snapshotFlow { anchoredState.progress(SlideOverAnchor.Center, edge) }
      .collect { progress ->
        transitionState.seekOrReset(progress, anchoredState.offset, targetNavStack, args)
      }
  }
}

/**
 * Runs [onSettled] for the newly active record once the transition settles, keyed on settle + args
 * so it fires on navigation, not every recomposition, and post-commit so the caller can mutate the
 * host map safely.
 *
 * `warmHit` reads a per-[args] snapshot of [warmReady] taken during composition, before the
 * promoted record's preload effect removes its key. The live set would always miss, since the key
 * is gone by settle.
 */
@Composable
private fun <T : NavArgument> SettleBookkeeping(
  hasSettled: Boolean,
  args: NavStackList<T>,
  warmReady: Set<String>,
  onSettled: (active: T, warmHit: Boolean) -> Unit,
) {
  val warmAtNav = remember(args) { warmReady.toSet() }
  LaunchedEffect(hasSettled, args) {
    if (!hasSettled) return@LaunchedEffect
    val active = args.active
    onSettled(active, active.key in warmAtNav)
  }
}

/**
 * Paces off-screen precomposition of the reachable adjacents into the shared record layout. A null
 * adjacent (swipe disallowed, or preload flag off) warms nothing; [PreloadAdjacentRecord] adopts
 * the warmed node when navigation reaches it. Presence is stable across a swipe so the node
 * survives to be adopted; pacing waits on [settledProvider] so it never competes with an
 * in-progress transition.
 */
@Composable
private fun <T : NavArgument> WarmAdjacentRecords(
  backwardAdjacent: T?,
  forwardAdjacent: T?,
  settledProvider: () -> Boolean,
  layoutState: SubcomposeLayoutState,
  hostFor: (T) -> RecordHost<T>,
  onWarmReady: (T, Boolean) -> Unit,
  onWarmFailed: (T, Throwable) -> Unit,
) {
  PreloadAdjacentRecord(
    target = backwardAdjacent,
    enabled = backwardAdjacent != null,
    settledProvider = settledProvider,
    layoutState = layoutState,
    warmContent = backwardAdjacent?.let { hostFor(it).slotContent },
    onReady = onWarmReady,
    onFailed = onWarmFailed,
  )
  PreloadAdjacentRecord(
    target = forwardAdjacent,
    enabled = forwardAdjacent != null,
    settledProvider = settledProvider,
    layoutState = layoutState,
    warmContent = forwardAdjacent?.let { hostFor(it).slotContent },
    onReady = onWarmReady,
    onFailed = onWarmFailed,
  )
}

/**
 * One record to subcompose and place in [RecordLayout], built from its [host].
 *
 * [placement] is applied to [host] in the measure pass, just before [host]'s
 * [RecordHost.slotContent] is subcomposed, so the snapshot write and its dependent read stay in the
 * same phase.
 *
 * @property placement where the record sits this pass; also drives its presenter/a11y state.
 * @property place places the measured placeable given the layout width, applying the record's
 *   animated slide offset or fade alpha.
 */
private class RecordSlot<T : NavArgument>(
  val host: RecordHost<T>,
  val placement: Placement,
  val place: Placeable.PlacementScope.(Placeable, Int) -> Unit,
) {
  val slotId: String
    get() = host.key

  val content: @Composable () -> Unit
    get() = host.slotContent
}

/**
 * Renders every nav event through ONE [SubcomposeLayout]: a slide (two records + scrim), a root
 * reset's crossfade (two records, no scrim), or the settled single active record — plus any
 * off-screen [warmAdjacents]. The offsets, alphas, and child visibility transitions are created in
 * the outer composition so they stay registered with the parent [Transition], and read at
 * placement.
 *
 * One call site for every event is what keeps the layout in composition. A root reset used to
 * render through its own [androidx.compose.animation.AnimatedContent], so entering and leaving one
 * took the layout out of composition and moved every record's `movableContentOf` to a second host
 * and back. The move preserves each record, but it hands the shared `_registry_` bookkeeping
 * between two hosting mechanisms on a frame the user is watching, and it drops any warmed adjacent
 * held by the layout.
 *
 * Every record the transition touches stays mounted in a stable append-only order until the
 * transition settles (see [mountTouchedRecords]); records not currently animating are parked
 * off-screen rather than removed. This keeps each record's per-record `movableContentOf` (and its
 * shared `SaveableStateHolder` `_registry_` key) subcomposed exactly once with a stable slot, so a
 * record is never torn down and re-registered mid-transition — the "used multiple times" crash.
 * Warm adjacents are placed off-screen too, so their interop views attach ahead of navigation, and
 * navigating to one moves its slot on-screen rather than composing a duplicate.
 *
 * @param event the event being animated, or null when settled.
 */
@Composable
@OptIn(ExperimentalTransitionApi::class, ExperimentalSharedTransitionApi::class)
private fun <T : NavArgument> Transition<NavStackList<T>>.Content(
  event: AnimatedNavEvent?,
  layoutState: SubcomposeLayoutState,
  warmAdjacents: List<WarmAdjacent<T>>,
  keepMountedKeys: Set<String>,
  canPruneMountedRecords: Boolean,
  hostFor: (T) -> RecordHost<T>,
) {
  val settled = event == null
  val style =
    if (event == AnimatedNavEvent.RootReset) TransitionStyle.Crossfade else TransitionStyle.Slide
  val background = if (settled) null else transitionBackground(event)
  val top = if (settled) currentState.active else transitionTop(event)
  // A real transition has two distinct records; equal keys (a reverse-flick whose current/target
  // states converged, or a root reset that kept the active record) render one record like a settle.
  val twoRecords = background != null && background.key != top.key

  // Keep every touched record mounted in a stable order until settle, so no slot is torn down and
  // re-registered mid-transition (see this function's KDoc). Warm adjacents join this same map
  // instead of getting their own slots, so one retention rule governs every slot and an async
  // preload teardown can't drop one on its own schedule.
  val mounted = remember { LinkedHashMap<String, T>() }
  mountTouchedRecords(mounted, settled, top, background, warmAdjacents)
  pruneMountedRecords(mounted, settled, canPruneMountedRecords, top, background, keepMountedKeys)

  val params = rememberTransitionParams(settled, style, twoRecords, top, background)
  // Only a slide dims what it covers; a crossfade shows both records at once, so a scrim would
  // darken the outgoing one.
  val scrimAlpha =
    if (twoRecords && style == TransitionStyle.Slide)
      animateFloat(label = "scrimAlpha", transitionSpec = { TransitionSpec }) { state ->
        if (state.active == background) 0f else 1f
      }
    else null

  val recordSlots =
    mounted.values.map { record -> mountedRecordSlot(hostFor(record), params, top.key) }
  val onScreen = onScreenSlots(recordSlots, twoRecords, top, background)

  RecordLayout(layoutState) { constraints ->
    measureAndPlaceRecords(recordSlots, onScreen, scrimAlpha, constraints)
  }
}

/**
 * The visible slots back-to-front: a two-record transition is [background, top]; otherwise just the
 * active [top]. Drawn from [recordSlots] since only mounted records are ever on screen.
 */
private fun <T : NavArgument> onScreenSlots(
  recordSlots: List<RecordSlot<T>>,
  twoRecords: Boolean,
  top: T,
  background: T?,
): List<RecordSlot<T>> =
  if (twoRecords) {
    val backgroundKey = requireNotNull(background) { "twoRecords implies a background record" }.key
    listOf(
      recordSlots.first { it.slotId == backgroundKey },
      recordSlots.first { it.slotId == top.key },
    )
  } else {
    listOf(recordSlots.first { it.slotId == top.key })
  }

/** How the two records of a transition animate between each other. */
private enum class TransitionStyle {
  /** The top record slides in from the trailing edge over a dimmed background record. */
  Slide,

  /** The records fade into each other in place. A root reset has no adjacent to slide from. */
  Crossfade,
}

/**
 * One animating record's key, visibility scope, and placement.
 *
 * @property key the record's key.
 * @property scope the record's [Navigation] visibility scope.
 * @property place places the record, applying its animated slide offset or fade alpha.
 */
private class TransitionRole(
  val key: String,
  val scope: AnimatedVisibilityScope,
  val place: Placeable.PlacementScope.(Placeable, Int) -> Unit,
)

/**
 * The transition state shared by every mounted record's slot. Both roles are null when settled;
 * [background] is null for a degenerate transition that renders one record.
 *
 * @property settled true when the transition has settled to a single active record.
 * @property top the incoming/active animating record, or null when settled.
 * @property background the outgoing animating record, or null when settled or degenerate.
 */
private class TransitionParams(
  val settled: Boolean,
  val top: TransitionRole?,
  val background: TransitionRole?,
)

/**
 * Builds the [TransitionParams] for the current transition. The animations and visibility scopes
 * are created here, in the outer composition, so they stay registered with the parent [Transition].
 */
@Composable
@OptIn(ExperimentalTransitionApi::class, ExperimentalSharedTransitionApi::class)
private fun <T : NavArgument> Transition<NavStackList<T>>.rememberTransitionParams(
  settled: Boolean,
  style: TransitionStyle,
  twoRecords: Boolean,
  top: T,
  background: T?,
): TransitionParams {
  val topRole =
    if (settled) null
    else {
      val scope = recordVisibilityScope(top)
      val place = rolePlacement(style, top, isBackground = false)
      TransitionRole(top.key, scope, place)
    }
  val backgroundRole =
    if (twoRecords) {
      val bg = requireNotNull(background) { "twoRecords implies a background record" }
      val scope = recordVisibilityScope(bg)
      val place = rolePlacement(style, bg, isBackground = true)
      TransitionRole(bg.key, scope, place)
    } else null
  return TransitionParams(settled, topRole, backgroundRole)
}

/**
 * The animated placement for [record] in its transition role. A [TransitionStyle.Slide] moves the
 * top record in from the trailing edge while the background record parallaxes the other way by
 * [EnterOffsetFraction]; a [TransitionStyle.Crossfade] leaves both in place and animates alpha.
 */
@Composable
private fun <T : NavArgument> Transition<NavStackList<T>>.rolePlacement(
  style: TransitionStyle,
  record: T,
  isBackground: Boolean,
): Placeable.PlacementScope.(Placeable, Int) -> Unit =
  when (style) {
    TransitionStyle.Slide -> {
      val awayOffset = if (isBackground) -EnterOffsetFraction else 1f
      val offset =
        animateFloat(
          label = if (isBackground) "backgroundSlideOffset" else "slideOffset",
          transitionSpec = { TransitionSpec },
        ) { state ->
          if (state.active == record) 0f else awayOffset
        }
      slideBy(offset)
    }
    TransitionStyle.Crossfade -> {
      val alpha =
        animateFloat(
          label = if (isBackground) "backgroundFadeAlpha" else "fadeAlpha",
          transitionSpec = { TransitionSpec },
        ) { state ->
          if (state.active == record) 1f else 0f
        }
      fadeBy(alpha)
    }
  }

/**
 * Builds the slot for a mounted record via its [host]. The record on screen this pass ([activeKey]
 * when settled, an animating role otherwise) is placed and active. Every other mounted record is
 * parked off screen with its presenter paused.
 *
 * The chosen [Placement] varies the record's transition scope and semantics within the adopted node
 * without moving its movableContentOf (see [RecordHost]). It is applied to the host in the measure
 * pass, not here.
 */
private fun <T : NavArgument> mountedRecordSlot(
  host: RecordHost<T>,
  params: TransitionParams,
  activeKey: String,
): RecordSlot<T> {
  val role =
    params.top?.takeIf { it.key == host.key } ?: params.background?.takeIf { it.key == host.key }
  return when {
    params.settled && host.key == activeKey ->
      RecordSlot(host, Placement.Settled) { placeable, _ -> placeable.place(0, 0) }
    role != null -> RecordSlot(host, Placement.Transitioning(role.scope), role.place)
    // Off screen this pass: an interrupted transition's leftover, or a retained adjacent.
    else -> RecordSlot(host, Placement.Parked) { placeable, width -> placeable.place(width, 0) }
  }
}

// Beyond this many parked records mid-slide, evict the oldest so a never-settling gesture chain
// can't grow [mounted] without bound. Sized well above what any real slide sequence reaches.
private const val MaxParkedRecords = 8

/**
 * Retains the slots navigation can still return to, and releases the rest one pass later.
 *
 * Retaining a slot deactivates it instead of disposing it, so its `LayoutNode` subtree and slot
 * table stay alive. Retaining unreachable slots therefore only grows: Circuit disposes a popped
 * record's provider, so that slot can never be subcomposed again.
 *
 * Releasing a slot the pass it goes stale is worse. Compose disposes it inside
 * `disposeOrReuseStartingFromIndex`, detaching the record's `AndroidView` mid-measure, which lets
 * platform focus recovery re-enter measurement. One retained pass lets Compose deactivate it out of
 * frame, so the pass that disposes it sees an already-detached subtree.
 *
 * Retaining past that buys no state: `takeNodeFromReusables` forces `setContentWithReuse`, which
 * discards all remembered content. Only staying subcomposed avoids a rebuild, so widening
 * [retainable] past the keep-mounted set costs heap and changes nothing.
 */
private class RecordSlotReusePolicy : SubcomposeSlotReusePolicy {

  /** Reachable keys, written from composition and read during layout. See [keepMountedKeys]. */
  var retainable: Set<String> = emptySet()

  // Slots that have had their one deactivation pass; the next pass disposes them.
  private val deactivating = mutableSetOf<Any?>()
  private val deactivatingNext = mutableSetOf<Any?>()

  override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
    deactivatingNext.clear()
    slotIds.retainAll { slotId ->
      when {
        slotId in retainable -> true
        slotId in deactivating -> false
        else -> deactivatingNext.add(slotId)
      }
    }
    deactivating.clear()
    deactivating.addAll(deactivatingNext)
  }

  // Donating a slot reassigns its id and recomposes the new record's content over that slot table,
  // removing the old record's movableContentOf. Only the fallback loop asks, never exact-key reuse,
  // so refusing costs nothing.
  override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean = false
}

// How many records either side of the active one keep their slot subcomposed. Circuit holds a
// record's movableContentOf for as long as its key stays in the nav stack, and backward/forward
// navigation never changes that key set, so dropping a slot and re-mounting the key later moves the
// content between two subcompositions. Two steps covers the swipes a user reaches without an
// intervening settle; past that the move is accepted rather than paying for another live screen.
private const val KeepMountedDepth = 2

/**
 * Keys that survive the settle prune in [pruneMountedRecords]. Without them the next pass re-mounts
 * the key the prune just dropped, rebuilding that screen and its interop views and relocating its
 * `movableContentOf` into the fresh subcomposition.
 *
 * Keyed on reachability from the committed stack, not on whether a record was warmed, so it holds
 * for a record that was never warmed and does not depend on the preload flag. [NavStackList.active]
 * is included for the one-pass commit gap `warmAdjacents` bridges: `args` commits the destination
 * before the transition follows.
 */
private fun <T : NavArgument> keepMountedKeys(args: NavStackList<T>): Set<String> = buildSet {
  add(args.active.key)
  args.backwardItems.take(KeepMountedDepth).forEach { add(it.key) }
  args.forwardItems.take(KeepMountedDepth).forEach { add(it.key) }
}

/**
 * State for the one `SubcomposeLayout` that hosts every record: the on-screen slide and settled
 * slots, and the warmed adjacents placed off screen. A record is subcomposed at most once per
 * measure pass, so its `movableContentOf` (and its shared `SaveableStateHolder` `_registry_`) is
 * only ever hosted once. Warming is a placement of the record's own slot, not a second composition.
 *
 * [keepMounted] also bounds the hosts and the mounted records, so a retained slot always has a live
 * host and never outlives what can be subcomposed again.
 */
@Composable
private fun rememberRecordLayoutState(keepMounted: Set<String>): SubcomposeLayoutState {
  val policy = remember { RecordSlotReusePolicy() }
  // An abandoned composition must not narrow the retained set, so publish only on commit.
  SideEffect { policy.retainable = keepMounted }
  return remember { SubcomposeLayoutState(policy) }
}

/**
 * Appends the records this transition touches, plus any [warm] adjacent, to [mounted]. Append-only
 * and order-stable, so a record's slot is never reordered mid-transition. A key already present
 * keeps its original instance and position, since re-putting it would swap the instance the slot
 * was subcomposed with.
 *
 * Keyed by record key, the only identity [NavArgument] defines. Two instances can share one key,
 * and mounting both would host that record twice and relocate its `movableContentOf`.
 */
private fun <T : NavArgument> mountTouchedRecords(
  mounted: LinkedHashMap<String, T>,
  settled: Boolean,
  top: T,
  background: T?,
  warm: List<WarmAdjacent<T>>,
) {
  mounted.getOrPut(top.key) { top }
  if (!settled && background != null) mounted.getOrPut(background.key) { background }
  warm.forEach { adj -> mounted.getOrPut(adj.record.key) { adj.record } }
}

/**
 * Prunes [mounted] to the active record and [keepMounted], only when [settled] and
 * [canPruneMountedRecords], so the null-event pass between argument commit and transition start
 * cannot dispose a record's interop views while the record layout is measuring.
 *
 * Settle normally bounds growth. As a backstop against a gesture chain that never settles,
 * mid-slide growth is capped by evicting the oldest parked record, never [top] or [background],
 * whose slots must stay subcomposed.
 */
private fun <T : NavArgument> pruneMountedRecords(
  mounted: LinkedHashMap<String, T>,
  settled: Boolean,
  canPruneMountedRecords: Boolean,
  top: T,
  background: T?,
  keepMounted: Set<String>,
) {
  if (settled) {
    if (!canPruneMountedRecords) return
    mounted.keys.retainAll { it == top.key || it in keepMounted }
  } else {
    capParkedGrowth(mounted, top, background)
  }
}

/** Evicts the oldest parked records until [mounted] is back under the cap. */
private fun <T : NavArgument> capParkedGrowth(
  mounted: LinkedHashMap<String, T>,
  top: T,
  background: T?,
) {
  val onScreenCount = if (background != null && background.key != top.key) 2 else 1
  while (mounted.size > MaxParkedRecords + onScreenCount) {
    val oldestParked = mounted.keys.firstOrNull { it != top.key && it != background?.key } ?: break
    mounted.remove(oldestParked)
  }
}

/**
 * Subcomposes every slot and places them. [onScreenOrdered] is the visible set back-to-front
 * (background then top, or the single settled record); the scrim is placed just under the
 * front-most record. Off-screen (parked and warm) slots place behind and don't affect the reported
 * size.
 */
private fun SubcomposeMeasureScope.measureAndPlaceRecords(
  slots: List<RecordSlot<*>>,
  onScreenOrdered: List<RecordSlot<*>>,
  scrimAlpha: State<Float>?,
  constraints: Constraints,
): MeasureResult {
  val placeableByKey = slots.associate { slot ->
    // Apply placement here, in the measure pass, so the host's snapshot write and the read inside
    // its slotContent happen in the same phase rather than during composition. Loop-free because
    // Placement is a data class: re-writing an equal value is a no-op, so a stable placement can't
    // invalidate the snapshot and retrigger measure.
    slot.host.placement = slot.placement
    slot.slotId to subcompose(slot.slotId, slot.content).first().measure(constraints)
  }
  val scrimPlaceable = subcomposeScrim(scrimAlpha)?.measure(constraints)
  val onScreenKeys = onScreenOrdered.mapTo(mutableSetOf()) { it.slotId }
  val onScreenPlaceables = onScreenOrdered.map { placeableByKey.getValue(it.slotId) }
  val size =
    constraints.constrain(
      IntSize(onScreenPlaceables.maxOf { it.width }, onScreenPlaceables.maxOf { it.height })
    )
  return layout(size.width, size.height) {
    slots.forEach { slot ->
      if (slot.slotId !in onScreenKeys) {
        slot.place(this, placeableByKey.getValue(slot.slotId), size.width)
      }
    }
    onScreenOrdered.forEachIndexed { index, slot ->
      if (index == onScreenOrdered.lastIndex) scrimPlaceable?.place(0, 0)
      slot.place(this, placeableByKey.getValue(slot.slotId), size.width)
    }
  }
}

/** The record drawn behind: the one being revealed going backward, else the one being covered. */
private fun <T : NavArgument> Transition<NavStackList<T>>.transitionBackground(
  event: AnimatedNavEvent
): T {
  val backward = event == AnimatedNavEvent.Pop || event == AnimatedNavEvent.Backward
  return if (backward) targetState.active else currentState.active
}

/** The record drawn in front: the one being dismissed going backward, else the one arriving. */
private fun <T : NavArgument> Transition<NavStackList<T>>.transitionTop(
  event: AnimatedNavEvent
): T {
  val backward = event == AnimatedNavEvent.Pop || event == AnimatedNavEvent.Backward
  return if (backward) currentState.active else targetState.active
}

private fun SubcomposeMeasureScope.subcomposeScrim(scrimAlpha: State<Float>?): Measurable? {
  return if (scrimAlpha != null) {
    subcompose(ScrimSlotId) {
        Canvas(Modifier.fillMaxSize()) {
          val alpha = scrimAlpha.value
          if (alpha > 0f) drawRect(Color.Black.copy(alpha = ScrimMaxAlpha * alpha))
        }
      }
      .first()
  } else null
}

/** Placement that slides a record horizontally by [offset] (a fraction of the layout width). */
private fun slideBy(offset: State<Float>): Placeable.PlacementScope.(Placeable, Int) -> Unit =
  { placeable, width ->
    placeable.placeWithLayer(0, 0, layerBlock = { translationX = offset.value * width })
  }

/** Placement that fades a record in place to [alpha], leaving it where it sits. */
private fun fadeBy(alpha: State<Float>): Placeable.PlacementScope.(Placeable, Int) -> Unit =
  { placeable, _ ->
    placeable.placeWithLayer(0, 0, layerBlock = { this.alpha = alpha.value })
  }

/**
 * The [Navigation] animated visibility scope for [navArg]'s transition. [createChildTransition]
 * must run in the outer composition to stay registered with the parent [Transition]; the scope is
 * handed to the record's [RecordHost] and read by its slot content during measure.
 */
@Composable
@OptIn(ExperimentalTransitionApi::class, ExperimentalSharedTransitionApi::class)
private fun <T : NavArgument> Transition<NavStackList<T>>.recordVisibilityScope(
  navArg: T
): AnimatedVisibilityScope {
  val visibility = createChildTransition { parentState ->
    targetEnterExit(visible = { it.active == navArg }, targetState = parentState)
  }
  return SimpleAnimatedVisibilityScope(visibility)
}

/** The single [SubcomposeLayout] that both places on-screen records and holds precomposed ones. */
@Composable
private fun RecordLayout(
  layoutState: SubcomposeLayoutState,
  measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult,
) {
  SubcomposeLayout(layoutState, modifier = Modifier.fillMaxSize(), measurePolicy = measurePolicy)
}
