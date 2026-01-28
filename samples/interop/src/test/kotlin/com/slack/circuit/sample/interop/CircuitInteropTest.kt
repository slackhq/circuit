// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import android.widget.TextView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode.NATIVE

@GraphicsMode(NATIVE)
@RunWith(RobolectricTestRunner::class)
class CircuitInteropTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private var expectedCount = 0

  private val drivers =
    mapOf(
      UiSource.Circuit to ComposeCounterUiDriver(composeTestRule),
      UiSource.View to EspressoCounterUiDriver(),
    )

  @Test
  fun smokeTest() {
    composeTestRule.run {
      for (presenterSource in PresenterSource.entries) {
        println("---Presenter source is now ${presenterSource.name}")
        for (uiSource in UiSource.entries) {
          println("---Ui source is now ${uiSource.name}")
          //          println("${presenterSource.name} — ${uiSource.name}")
          changePresenter(presenterSource)
          changeUi(uiSource)
          println(getCount())
          assertCount()
          increment()
          println(getCount())
          assertCount()
          decrement()
          println(getCount())
          assertCount()
        }
      }
    }
  }

  private fun SemanticsNodeInteraction.getText(): String {
    val node = fetchSemanticsNode()
    val actual = mutableListOf<String>()
    node.config.getOrNull(SemanticsProperties.EditableText)?.let { actual.add(it.text) }
    node.config.getOrNull(SemanticsProperties.Text)?.let {
      actual.addAll(it.map { anStr -> anStr.text })
    }
    return actual.first()
  }

  private fun assertCount() {
    assertThat(getCount()).isEqualTo(expectedCount)
    // TODO why doesn't this work? It sees the text but doesn't match
    // composeTestRule.onNodeWithTag(TestTags.COUNT).assertTextContains(expectedCount.toString())
  }

  private fun getCount(): Int = drivers.getValue(currentUiSource()).getCount()

  private fun currentUiSource(): UiSource {
    val text = composeTestRule.onNodeWithTag(TestTags.currentSourceFor(UiSource.LABEL)).getText()
    return UiSource.entries.find { it.presentationName == text }
      ?: error("Could not find UiSource matching presentation text '$text'")
  }

  private fun currentPresenterSource(): PresenterSource {
    val text =
      composeTestRule.onNodeWithTag(TestTags.currentSourceFor(PresenterSource.LABEL)).getText()
    return PresenterSource.entries.find { it.presentationName == text }
      ?: error("Could not find PresenterSource matching presentation text '$text'")
  }

  private fun increment() {
    println("inc ⬆⬆")
    expectedCount++
    drivers.getValue(currentUiSource()).increment()
  }

  private fun decrement() {
    println("dec ⬇⬇")
    expectedCount--
    drivers.getValue(currentUiSource()).decrement()
  }

  private fun changePresenter(source: PresenterSource) {
    if (currentPresenterSource() == source) {
      return
    }

    // do by ordinal/index instead?
    composeTestRule.onNodeWithTag(TestTags.PRESENTER_DROPDOWN).performClick()
    composeTestRule.onNodeWithText(source.presentationName).performClick()
    composeTestRule.waitForIdle()

    // Assert that our source changed
    assertThat(currentPresenterSource()).isEqualTo(source)

    // Assert that the count was preserved
    assertCount()
  }

  private fun changeUi(source: UiSource) {
    if (currentUiSource() == source) {
      return
    }

    composeTestRule.onNodeWithTag(TestTags.UI_DROPDOWN).performClick()
    composeTestRule.onNodeWithText(source.presentationName).performClick()

    // Assert that our source changed
    assertThat(currentUiSource()).isEqualTo(source)

    // Assert that the count was preserved
    assertCount()
  }

  sealed interface CounterUiDriver {
    fun getCount(): Int

    fun increment()

    fun decrement()
  }

  inner class ComposeCounterUiDriver(private val composeTestRule: ComposeTestRule) :
    CounterUiDriver {
    override fun getCount(): Int {
      return composeTestRule
        .onNodeWithTag(TestTags.COUNT)
        .getText()
        .substringAfter(" ")
        .trim()
        .toInt()
    }

    override fun increment() {
      composeTestRule.onNodeWithTag(TestTags.INCREMENT).performClick()
    }

    override fun decrement() {
      composeTestRule.onNodeWithTag(TestTags.DECREMENT).performClick()
    }
  }

  class EspressoCounterUiDriver : CounterUiDriver {
    override fun getCount(): Int {
      var count = 0
      onView(withId(R.id.count)).check { view, _ ->
        count = (view as TextView).text.toString().substringAfter(" ").trim().toInt()
      }
      return count
    }

    override fun increment() {
      onView(withId(R.id.increment)).perform(click())
    }

    override fun decrement() {
      onView(withId(R.id.decrement)).perform(click())
    }
  }
}
