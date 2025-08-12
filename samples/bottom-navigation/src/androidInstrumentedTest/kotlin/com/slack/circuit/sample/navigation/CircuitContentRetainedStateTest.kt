package com.slack.circuit.sample.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.internal.test.TestContentTags
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.retained.rememberRetained
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CircuitContentRetainedStateTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  @Test
  fun retainedStateWithKeys() = retainedStateScopedToBackstack(true)

  @Test
  fun retainedStateWithoutKeys() = retainedStateScopedToBackstack(false)

  private fun retainedStateScopedToBackstack(useKeys: Boolean) = runTest {
    val circuit =
      createTestCircuit(useKeys = useKeys, rememberType = TestCountPresenter.RememberType.Retained)
    val content =
      @Composable {
        CircuitCompositionLocals(circuit) { CircuitContent(screen = TestScreen.ScreenA) }
      }

    with(composeTestRule) {
      setActivityContent(content)

      // Current: Screen A. Increase count to 3.
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TestContentTags.TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("1")
      onNodeWithTag(TestContentTags.TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("2")
      onNodeWithTag(TestContentTags.TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("3")

      // Restart the activity
      scenario.recreate()
      setActivityContent(content)

      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("3")
      onNodeWithTag(TestContentTags.TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("4")
      onNodeWithTag(TestContentTags.TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("5")
      onNodeWithTag(TestContentTags.TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("6")

      // Restart the activity
      scenario.recreate()
      setActivityContent(content)

      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TestContentTags.TAG_COUNT).assertTextEquals("6")
    }
  }

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity -> activity.setContent { content() } }
  }
}