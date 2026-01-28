// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.effects

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val TAG_COLUMN = "column"

@RunWith(RobolectricTestRunner::class)
class ImpressionEffectScrollTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val registry = RetainedStateRegistry()

  @Test
  fun scrollImpressionEffect() {
    testScrollingImpression { index, counts ->
      ImpressionEffect { counts[index] = counts.getOrPut(index) { 0 } + 1 }
    }
  }

  @Test
  fun scrollLaunchedImpressionEffect() {
    testScrollingImpression { index, counts ->
      LaunchedImpressionEffect { counts[index] = counts.getOrPut(index) { 0 } + 1 }
    }
  }

  private fun testScrollingImpression(content: @Composable (Int, MutableMap<Int, Int>) -> Unit) {
    composeTestRule.run {
      val counts = mutableMapOf<Int, Int>()
      setRetainedContent {
        LazyColumn(Modifier.testTag(TAG_COLUMN).size(200.dp)) {
          items(40) { index ->
            Row(Modifier.fillMaxWidth().height(20.dp)) {
              content(index, counts)
              Text(text = "$index")
            }
          }
        }
      }
      // Verify initial impressions are counted.
      assertEquals(10, counts.size)
      assertTrue(counts.all { it.value == 1 })
      for ((key, value) in counts) {
        assertTrue(value == 1, "Value at index $key != 1, is $value")
      }
      // Scroll to remove all initial rows, verify impressions are counted.
      onNodeWithTag(TAG_COLUMN).performScrollToIndex(20)
      assertEquals(20, counts.size)
      for ((key, value) in counts) {
        assertTrue(value == 1, "Value at index $key != 1, is $value")
      }
      // Scroll back to the start and verify new impressions.
      onNodeWithTag(TAG_COLUMN).performScrollToIndex(0)
      for ((key, value) in counts) {
        if (key >= 10) {
          assertTrue(value == 1, "Value at index $key != 1, is $value")
        } else {
          assertTrue(value == 2, "Value at index $key != 2, is $value")
        }
      }
    }
  }

  private fun ComposeContentTestRule.setRetainedContent(content: @Composable () -> Unit = {}) {
    setContent {
      CompositionLocalProvider(LocalRetainedStateRegistry provides registry) { content() }
    }
  }
}
