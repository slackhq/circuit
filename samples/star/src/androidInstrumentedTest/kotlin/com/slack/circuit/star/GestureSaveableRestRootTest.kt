package com.slack.circuit.star

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.test.TestContentTags
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.AndroidPredictiveBackNavDecorator
import com.slack.circuitx.gesturenavigation.CupertinoGestureNavigationDecorator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GestureSaveableRestRootTest {

  @get:Rule val composeTestRule = createComposeRule()

  /*
  What happens:
  - Record A is set as args
  - Record A removed from args, Record B is args
  - Record A is in composition until transition completes
  - Record B removed from args, Record A is args
  - Record A is not in previousContentProviders
  - Record A has a new content provider created, but the key is retained
  - RecordContentProviders are not equal because they have different content
  - Transition has Record A -> Record A with different content which is !=
  - Animated content is rendered for both, saveable blows up
   */
  @Test
  fun testReset() = runTest {
    lateinit var navigator: Navigator
    composeTestRule.run {
      setTestContent { TestContent { navigator = it } }
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.RootAlpha.label)
      mainClock.autoAdvance = false
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      navigator.resetRoot(newRoot = TestScreen.ScreenB, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      navigator.resetRoot(newRoot = TestScreen.ScreenC, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      navigator.resetRoot(newRoot = TestScreen.ScreenB, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
      mainClock.autoAdvance = true
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.ScreenA.label)
    }
  }

  @Test
  fun testMovable() = runTest {
    composeTestRule.run {
      lateinit var state: SnapshotStateList<TestScreen>
      setContent {
        val backStack = remember {
          mutableStateListOf<TestScreen>(TestScreen.RootAlpha).also { state = it }
        }
        val providers = buildContentProviders(backStack)
        val commonState = remember { CommonState() }
        val transition = updateTransition(providers.last())
        println("AND ${transition.currentState.first} -> ${transition.targetState.first}")
        transition.AnimatedContent(
          modifier = Modifier.fillMaxSize(),
          transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { (screen, content) ->
          DisposableEffect(screen) {
            println("AND Entered $screen")
            onDispose { println("AND Disposed $screen") }
          }
          content(screen, commonState)
        }
      }
      fun resetRoot(screen: TestScreen) {
        Snapshot.withMutableSnapshot {
          state.clear()
          state.add(screen)
        }
      }
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.RootAlpha.label)
      mainClock.autoAdvance = false
      resetRoot(TestScreen.ScreenA)
      mainClock.advanceTimeByFrame()
      resetRoot(TestScreen.ScreenB)
      mainClock.advanceTimeByFrame()
      resetRoot(TestScreen.ScreenA)
      mainClock.advanceTimeByFrame()
      resetRoot(TestScreen.ScreenC)
      mainClock.advanceTimeByFrame()
      resetRoot(TestScreen.ScreenA)
      mainClock.advanceTimeByFrame()
      resetRoot(TestScreen.ScreenB)
      mainClock.advanceTimeByFrame()
      resetRoot(TestScreen.ScreenA)
      mainClock.autoAdvance = true
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.ScreenA.label)
    }
  }

  @SuppressLint("NewApi")
  @OptIn(ExperimentalSharedTransitionApi::class)
  private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
    val circuit =
      createTestCircuit(rememberType = TestCountPresenter.RememberType.Saveable)
        .newBuilder()
        .setAnimatedNavDecoratorFactory(CupertinoGestureNavigationDecorator.Factory {})
        .setAnimatedNavDecoratorFactory(AndroidPredictiveBackNavDecorator.Factory {})
        .setAnimatedNavDecoratorFactory(NavigatorDefaults.DefaultDecoratorFactory)
        .build()
    setContent {
      PreviewSharedElementTransitionLayout { CircuitCompositionLocals(circuit) { content() } }
    }
  }
}

@Composable
private fun buildContentProviders(
  backStack: SnapshotStateList<TestScreen>
): ImmutableList<Pair<TestScreen, @Composable (TestScreen, CommonState) -> Unit>> {
  val previousContent = remember {
    mutableMapOf<TestScreen, @Composable (TestScreen, CommonState) -> Unit>()
  }
  return backStack
    .map { screen -> screen to previousContent.getOrPut(screen) { createRecordContent() } }
    .toImmutableList()
    .also { list ->
      previousContent.clear()
      for (provider in list) {
        previousContent[provider.first] = provider.second
      }
    }
}

private fun createRecordContent() =
  //  @Composable { screen ->
  movableContentOf<TestScreen, CommonState> { screen, commonState ->
    val key = currentCompositeKeyHash
    remember {
      object : RememberObserver {
        override fun onAbandoned() {
          println("RC onAbandoned $screen $key ${hashCode()}")
        }

        override fun onForgotten() {
          println("RC onForgotten $screen $key ${hashCode()}")
        }

        override fun onRemembered() {
          println("RC onRemembered $screen $key ${hashCode()}")
        }
      }
    }
    commonState.State(screen)
    BasicText(text = screen.label, modifier = Modifier.testTag(TestContentTags.TAG_LABEL))
  }

@Stable
class CommonState {
  private val active = mutableSetOf<Any>()

  @Composable
  fun State(key: Any) {
    ReusableContent(key) {
      DisposableEffect(Unit) {
        require(key !in active) { "Key $key was used multiple times " }
        active += key
        onDispose { active -= key }
      }
    }
  }
}

@Composable
private fun TestContent(liftNavigator: (Navigator) -> Unit) {
  Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
    val backStack = rememberSaveableBackStack(root = TestScreen.RootAlpha)
    val navigator = rememberCircuitNavigator(backStack = backStack)
    SideEffect { liftNavigator(navigator) }
    NavigableCircuitContent(navigator, backStack, modifier = Modifier)
  }
}
