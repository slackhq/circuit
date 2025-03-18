package com.slack.circuit.sample.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationTest {

  @get:Rule(order = 1) val robolectricRule = AddActivityToRobolectricRule()
  @get:Rule(order = 2) val composeTestRule = createComposeRule()

  @Test
  fun `Test savable, fast multi-root reset`() = runTest {
    val tabs = TabScreen.all
    lateinit var navigator: Navigator
    composeTestRule.setTestContent(tabs) {
      val backStack = rememberSaveableBackStack(tabs.first())
      navigator = rememberCircuitNavigator(backStack)
      ContentScaffold(backStack, navigator, tabs, Modifier.fillMaxSize())
    }
    with(composeTestRule) {
      // Check the title
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.root.label)
      onAllNodesWithTag(ContentTags.TAG_TAB).filterToOne(hasText(TabScreen.root.label))

      // Setup backstacks
      repeat(6) { onNodeWithTag(ContentTags.TAG_CONTENT).performClick() }
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.screen2.label)
      onAllNodesWithTag(ContentTags.TAG_TAB)
        .filterToOne(hasText(TabScreen.screen1.label))
        .performClick()
      repeat(6) { onNodeWithTag(ContentTags.TAG_CONTENT).performClick() }
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.screen3.label)
      mainClock.autoAdvance = false
      mainClock.advanceTimeByFrame()
      repeat(10) {
        onAllNodesWithTag(ContentTags.TAG_TAB)
          .filterToOne(hasText(TabScreen.root.label))
          .performClick()
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        onAllNodesWithTag(ContentTags.TAG_TAB)
          .filterToOne(hasText(TabScreen.screen1.label))
          .performClick()
        mainClock.advanceTimeByFrame()
        onAllNodesWithTag(ContentTags.TAG_TAB)
          .filterToOne(hasText(TabScreen.screen3.label))
          .performClick()
        mainClock.advanceTimeByFrame()
        onAllNodesWithTag(ContentTags.TAG_TAB)
          .filterToOne(hasText(TabScreen.screen2.label))
          .performClick()
        mainClock.advanceTimeByFrame()
      }
      mainClock.autoAdvance = true
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.screen2.label)
    }
  }

  @SuppressLint("NewApi")
  @OptIn(ExperimentalSharedTransitionApi::class)
  private fun ComposeContentTestRule.setTestContent(
    tabs: List<TabScreen>,
    content: @Composable () -> Unit,
  ) {
    val circuit = buildCircuitForTabs(tabs)
    setContent {
      PreviewSharedElementTransitionLayout { CircuitCompositionLocals(circuit) { content() } }
    }
  }
}
