package com.slack.circuit.star

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
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
      repeat(10) {
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
      }
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
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
        .setAnimatedNavDecoratorFactory(NavigatorDefaults.DefaultDecoratorFactory)
        .setAnimatedNavDecoratorFactory(AndroidPredictiveBackNavDecorator.Factory {})
        .build()
    setContent {
      PreviewSharedElementTransitionLayout { CircuitCompositionLocals(circuit) { content() } }
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
