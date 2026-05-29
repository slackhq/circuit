// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sample.inbox.detail.DetailNavStyle
import com.slack.circuit.sample.inbox.detail.EmailDetailPane
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import com.slack.circuit.sample.inbox.detail.EmptyDetailPane
import com.slack.circuit.sample.inbox.list.InboxListPane
import com.slack.circuit.sample.inbox.list.InboxListScreen
import com.slack.circuit.sample.inbox.list.LocalSelectedEmailId
import dev.zacsweers.metro.AppScope

/**
 * Adaptive UI for the composite [InboxScreen].
 *
 * Layout is the only place this app cares about window size:
 * - **Compact / Medium width**: single-pane. We show the detail pane when there's a selection,
 *   otherwise the list. A `BackHandler` clears the selection.
 * - **Expanded width** (~840dp+): two-pane list-detail. The detail pane shows [EmptyDetailPane]
 *   when nothing is selected.
 *
 * The composite presenter is layout-agnostic — it just exposes `listState` and (optional)
 * `detailState`. Because `selectedEmailId` lives in `rememberRetained`, resizing the window
 * across the breakpoint preserves the user's current selection. The UI swaps between
 * single-pane and two-pane rendering, but the user's selection stays put.
 *
 * **Transitions:**
 * - In compact mode the list ↔ detail swap is driven by Circuit's *own* default
 *   [NavDecoration]: we read it off [LocalCircuit] and call [NavDecoration.DecoratedContent]
 *   with a synthetic [NavStackList] (`[List]` when nothing is selected, `[Detail, List]` when
 *   something is). The decoration diffs the stack and produces the same forward/pop animations
 *   you'd get from a real `NavigableCircuitContent` — including any per-screen
 *   `AnimatedScreenTransform`s registered on the Circuit. The child presenters never know any
 *   of this is happening; they just call `navigator.goTo()` / `navigator.pop()` and the
 *   composite intercepts those.
 * - The compact ↔ expanded layout change cross-fades.
 *
 * The current selection id is published via [LocalSelectedEmailId] so [InboxListPane] can
 * highlight the open conversation in the two-pane layout without it being threaded as an
 * argument.
 */
@CircuitInject(InboxScreen::class, AppScope::class)
@Composable
fun InboxHomeUi(state: InboxScreen.State, modifier: Modifier = Modifier) {
  val isExpanded =
    calculateWindowSizeClass().widthSizeClass == WindowWidthSizeClass.Expanded

  // Hoist the list scroll state above everything so it survives the compact ↔ expanded swap and
  // configuration changes.
  val listScrollState =
    rememberRetainedSaveable(saver = LazyListState.Saver) { LazyListState() }

  CompositionLocalProvider(LocalSelectedEmailId provides state.selectedEmailId) {
    AnimatedContent(
      targetState = isExpanded,
      modifier = modifier,
      transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
      label = "InboxLayout",
    ) { expanded ->
      if (expanded) {
        Row(Modifier.fillMaxSize()) {
          InboxListPane(
            state = state.listState,
            modifier = Modifier.width(360.dp).fillMaxHeight(),
            scrollState = listScrollState,
          )
          VerticalDivider()
          val detail = state.detailState
          if (detail != null) {
            EmailDetailPane(
              state = detail,
              modifier = Modifier.fillMaxSize(),
              navStyle = DetailNavStyle.Close,
            )
          } else {
            EmptyDetailPane(modifier = Modifier.fillMaxSize())
          }
        }
      } else {
        CompactSinglePane(state = state, listScrollState = listScrollState)
      }
    }
  }
}

@Composable
private fun CompactSinglePane(state: InboxScreen.State, listScrollState: LazyListState) {
  BackHandlerForSelection(active = state.detailState != null) {
    state.eventSink(InboxScreen.Event.ClearSelection)
  }

  // Synthetic nav stack: list at the root, detail on top when something is selected.
  val args: NavStackList<InboxPane> =
    remember(state.selectedEmailId) {
      val id = state.selectedEmailId
      if (id == null) navStackListOf(InboxPane.List)
      else navStackListOf(forwardItems = emptyList(), activeItem = InboxPane.Detail(id), backwardItems = listOf(InboxPane.List))
    }

  // Use Circuit's configured default decoration if available; otherwise (e.g. previews) fall back.
  val decoration: NavDecoration = LocalCircuit.current?.defaultNavDecoration ?: FadeNavDecoration

  decoration.DecoratedContent(args = args, modifier = Modifier.fillMaxSize()) { pane ->
    when (pane) {
      InboxPane.List ->
        // The list may remain composed while the detail pane animates in/out, so keep selection
        // highlighting ambient and harmless rather than special-casing compact mode.
        InboxListPane(
          state = state.listState,
          modifier = Modifier.fillMaxSize(),
          scrollState = listScrollState,
        )
      is InboxPane.Detail ->
        // detailState may briefly be null while the pop animates out — guard with the snapshot.
        state.detailState?.let { detail ->
          EmailDetailPane(
            state = detail,
            modifier = Modifier.fillMaxSize(),
            navStyle = DetailNavStyle.Back,
          )
        }
    }
  }
}

/** Synthetic [NavArgument]s for the compact single-pane "stack." Identity/keying only. */
private sealed interface InboxPane : NavArgument {
  data object List : InboxPane {
    override val key: String = "inbox-list"
    override val screen: Screen = InboxListScreen()
  }

  data class Detail(val emailId: String) : InboxPane {
    override val key: String = "email-detail-$emailId"
    override val screen: Screen = EmailDetailScreen(emailId)
  }
}

/** Minimal fallback decoration for environments without a Circuit (e.g. `@Preview`). */
private object FadeNavDecoration : NavDecoration {
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    AnimatedContent(
      targetState = args.active,
      modifier = modifier,
      transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
      contentKey = { it.key },
      label = "FadeNavDecoration",
    ) { content(it) }
  }
}

/**
 * Cross-platform "back to clear selection" hook. Hooked up on Android via an actual
 * `BackHandler`; a no-op on desktop where the gesture isn't available.
 */
@Composable expect fun BackHandlerForSelection(active: Boolean, onBack: () -> Unit)
