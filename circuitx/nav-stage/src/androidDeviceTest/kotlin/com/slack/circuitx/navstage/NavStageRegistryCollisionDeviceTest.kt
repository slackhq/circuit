// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalNavStageApi::class, ExperimentalSharedTransitionApi::class)

package com.slack.circuitx.navstage

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import org.junit.Test

/**
 * On-device fuzz for the `_registry_<key> was used multiple times` crash.
 *
 * [NavigableCircuitContent] keys every record's saveable state on one shared `SaveableStateHolder`,
 * so composing a record in two places at once throws. NavStage can do that when a pane slide still
 * holds an outgoing record that another pane now shows, or when a stage transition holds a record
 * in the exiting stage while the entering stage shows it in a different pane. This interrupts pane
 * slides, back seeks, and fold/unfold stage moves with each other.
 *
 * Needs real frame timing: the compose test clock and Robolectric serialize composition and never
 * produce the overlapping frames. So this drives a raw [ActivityScenario] on the real Choreographer
 * with heavy records that overrun a frame, and posts back events and navigation to the main looper
 * at jittered times instead of running them synchronously.
 */
class NavStageRegistryCollisionDeviceTest {

  @Test
  fun interruptedPaneAndStageTransitionsComposeEachRecordOnce() {
    val crash = AtomicReference<Throwable?>(null)
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
      crash.compareAndSet(null, throwable)
    }

    val multiPane = mutableStateOf(true)
    val mounts = mutableMapOf<FuzzScreen, Int>()
    val nextId = AtomicInteger(1)
    val navigatorRef = AtomicReference<Navigator?>(null)
    val scenario = ActivityScenario.launch(ComponentActivity::class.java)
    try {
      lateinit var dispatcher: OnBackPressedDispatcher
      scenario.onActivity { activity ->
        dispatcher = activity.onBackPressedDispatcher
        dispatcher.addCallback(
          object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
          }
        )
        activity.setContent { FuzzHost(multiPane, mounts, navigatorRef::set) }
        requireNotNull(navigatorRef.get()) { "navigator never captured" }.fillStack(nextId)
      }

      val instrumentation = InstrumentationRegistry.getInstrumentation()
      instrumentation.waitForIdleSync()
      val navigator = requireNotNull(navigatorRef.get()) { "navigator never captured" }
      val handler = Handler(Looper.getMainLooper())
      val rng = Random(0x57A6E)

      repeat(CYCLES) { cycle ->
        if (crash.get() != null) return@repeat
        val script = Script(handler, rng)
        when (cycle % 5) {
          0 -> {
            script.post { navigator.goTo(FuzzDetail(nextId.getAndIncrement())) }
            script.post { navigator.goTo(FuzzDetail(nextId.getAndIncrement())) }
            script.post { navigator.pop() }
          }
          1 -> {
            script.post { navigator.goTo(FuzzDetail(nextId.getAndIncrement())) }
            script.backStart(dispatcher)
            script.backProgress(dispatcher, from = 0f, to = 0.5f)
            script.post { dispatcher.onBackPressed() }
          }
          2 -> {
            script.backStart(dispatcher)
            script.backProgress(dispatcher, from = 0f, to = 0.4f)
            script.post { multiPane.toggle() }
            script.backProgress(dispatcher, from = 0.4f, to = 0.8f)
            script.post {
              if (rng.nextBoolean()) dispatcher.onBackPressed()
              else dispatcher.dispatchOnBackCancelled()
            }
          }
          3 -> {
            script.post { navigator.goTo(FuzzDetail(nextId.getAndIncrement())) }
            script.post { multiPane.toggle() }
            script.post { navigator.pop() }
            script.post { multiPane.toggle() }
          }
          else -> {
            script.backStart(dispatcher)
            script.backProgress(dispatcher, from = 0f, to = 0.3f)
            script.post { navigator.resetRoot(FuzzList(cycle)) }
            script.post { dispatcher.dispatchOnBackCancelled() }
          }
        }
        script.post { navigator.fillStack(nextId) }
        SystemClock.sleep(script.elapsed + REST_MIN_MS + rng.nextInt(REST_JITTER_MS))
        if (cycle % GC_EVERY == 0) Runtime.getRuntime().gc()
      }
      Thread { instrumentation.waitForIdleSync() }.apply { start() }.join(IDLE_TIMEOUT_MS)
      crash.get()?.let { throw AssertionError("NavStage crashed mid-transition", it) }

      SystemClock.sleep(SETTLE_MS)
      instrumentation.waitForIdleSync()
      lateinit var expected: Map<FuzzScreen, Int>
      lateinit var composed: Map<FuzzScreen, Int>
      scenario.onActivity {
        val stack = navigator.peekBackStack()
        val top = stack.first() as FuzzScreen
        val visible =
          if (multiPane.value && top is FuzzDetail) listOf(stack.last() as FuzzScreen, top)
          else listOf(top)
        expected = visible.associateWith { 1 }
        composed = mounts.filterValues { it != 0 }
      }
      assertEquals(expected, composed)
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(previousHandler)
      if (crash.get() == null) scenario.close()
    }
  }
}

private fun MutableState<Boolean>.toggle() {
  value = !value
}

private fun Navigator.fillStack(nextId: AtomicInteger) {
  val depth = peekBackStack().size
  repeat(MIN_DEPTH - depth) { goTo(FuzzDetail(nextId.getAndIncrement())) }
  repeat(depth - MAX_DEPTH) { pop() }
}

/** Posts each step to the main looper a jittered few milliseconds after the previous one. */
private class Script(private val handler: Handler, private val rng: Random) {
  private val start = SystemClock.uptimeMillis()
  var elapsed = 0L
    private set

  fun post(block: () -> Unit) {
    elapsed += STEP_MIN_MS + rng.nextInt(STEP_JITTER_MS)
    handler.postAtTime(block, start + elapsed)
  }

  fun backStart(dispatcher: OnBackPressedDispatcher) = post {
    dispatcher.dispatchOnBackStarted(backEvent(0f))
  }

  fun backProgress(dispatcher: OnBackPressedDispatcher, from: Float, to: Float) {
    for (step in 1..SEEK_STEPS) {
      val progress = from + (to - from) * step / SEEK_STEPS
      post { dispatcher.dispatchOnBackProgressed(backEvent(progress)) }
    }
  }

  private fun backEvent(progress: Float) =
    BackEventCompat(
      touchX = progress * BACK_TOUCH_WIDTH,
      touchY = 0f,
      progress = progress,
      swipeEdge = BackEventCompat.EDGE_LEFT,
    )
}

@Composable
private fun FuzzHost(
  multiPane: State<Boolean>,
  mounts: MutableMap<FuzzScreen, Int>,
  onNavigator: (Navigator) -> Unit,
) {
  val circuit = remember {
    Circuit.Builder()
      .addPresenterFactory { _, _, _ -> presenterOf { FuzzState } }
      .addUiFactory { screen, _ ->
        ui<FuzzState> { _, modifier -> FuzzRecord(screen as FuzzScreen, mounts, modifier) }
      }
      .build()
  }
  CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
    SharedElementTransitionLayout {
      val navStack = rememberSaveableNavStack(listOf<Screen>(FuzzList(-1)))
      val navigator = rememberCircuitNavigator(navStack = navStack, onRootPop = {})
      onNavigator(navigator)
      NavigableCircuitContent(
        navigator = navigator,
        navStack = navStack,
        modifier = Modifier.fillMaxSize(),
        decoration =
          remember {
            NavStageDecoration(
              strategies =
                listOf(
                  ListDetailNavStageStrategy(
                    isListPane = { it is TestListPane },
                    isDetailPane = { it is TestDetailPane },
                    isMultiPane = { multiPane.value },
                  )
                ),
              stageTransition = GestureNavStageTransition(),
            )
          },
      )
    }
  }
}

@Composable
private fun FuzzRecord(
  screen: FuzzScreen,
  mounts: MutableMap<FuzzScreen, Int>,
  modifier: Modifier,
) {
  DisposableEffect(screen) {
    mounts.merge(screen, 1, Int::plus)
    onDispose { mounts.merge(screen, -1, Int::plus) }
  }
  Column(modifier.fillMaxSize()) {
    BasicText("record $screen")
    LazyColumn(Modifier.fillMaxSize()) {
      items(items = (0 until ROW_COUNT).toList(), key = { "row_${screen}_$it" }) { row ->
        HeavyRow(id = screen.id, row = row)
      }
    }
  }
}

@Composable
private fun HeavyRow(id: Int, row: Int) {
  val work = remember(id, row) { burn(BURN_ITERATIONS + row) }
  Column {
    BasicText("row $id/$row #$work")
    NestedRows(depth = NEST_DEPTH, id = id, row = row)
  }
}

@Composable
private fun NestedRows(depth: Int, id: Int, row: Int) {
  if (depth == 0) {
    Column {
      repeat(LEAF_FANOUT) { leaf ->
        val local = rememberSaveable { leaf * 131 + id * 7 + row }
        BasicText("leaf $id/$row/$leaf ($local)")
      }
    }
    return
  }
  Column {
    val local = rememberSaveable { depth * 31 + id * 7 + row }
    BasicText("row $depth ($local)")
    NestedRows(depth - 1, id, row)
  }
}

private fun burn(iterations: Int): Long {
  var acc = 0L
  for (i in 0 until iterations * 1000) {
    acc += (i.toLong() * 2654435761L) xor (acc shr 3)
  }
  return acc
}

private sealed interface FuzzScreen : Screen {
  val id: Int
}

private data class FuzzList(override val id: Int) : FuzzScreen, TestListPane

private data class FuzzDetail(override val id: Int) : FuzzScreen, TestDetailPane

private data object FuzzState : CircuitUiState

private const val CYCLES = 600
private const val MIN_DEPTH = 3
private const val MAX_DEPTH = 6
private const val SEEK_STEPS = 4
private const val BACK_TOUCH_WIDTH = 400f
private const val STEP_MIN_MS = 2L
private const val STEP_JITTER_MS = 12
private const val REST_MIN_MS = 24L
private const val REST_JITTER_MS = 96
private const val GC_EVERY = 7
private const val IDLE_TIMEOUT_MS = 10_000L
private const val SETTLE_MS = 1_000L
private const val ROW_COUNT = 16
private const val NEST_DEPTH = 4
private const val LEAF_FANOUT = 4
private const val BURN_ITERATIONS = 30

private interface TestListPane

private interface TestDetailPane
