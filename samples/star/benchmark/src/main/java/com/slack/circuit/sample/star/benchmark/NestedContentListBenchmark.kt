// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.star.benchmark

import android.content.Intent
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Macrobenchmark that measures performance in a list with simple items and nested CircuitContents.
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(Parameterized::class)
class NestedContentListBenchmark(private val useNestedContent: Boolean) {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  companion object {
    @Suppress("ArrayPrimitive") // Required for Parameterized to work
    @JvmStatic
    @Parameters(name = "useNestedContent = {0}, iterations = {1}")
    fun data() = listOf(arrayOf(false), arrayOf(true))
  }

  @Test
  @Suppress("MagicNumber")
  fun benchmarkStartup() {
    benchmarkRule.measureRepeated(
      packageName = "com.slack.circuit.sample.star.apk",
      metrics =
        listOf(
          StartupTimingMetric(),
          FrameTimingMetric(),
          MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
        ),
      iterations = 10,
      startupMode = StartupMode.WARM,
    ) {
      pressHome()
      startActivityAndWait(
        Intent()
          .setClassName("com.slack.circuit.sample.star.apk", "com.slack.circuit.star.MainActivity")
          .putExtra("benchmark", true)
          .putExtra("scenario", "list")
          .putExtra("useNestedContent", useNestedContent)
      )

      device.wait(Until.hasObject(By.scrollable(true)), 5_000)

      val scrollableObject = device.findObject(By.scrollable(true))
      if (scrollableObject == null) {
        TestCase.fail("No scrollable view found in hierarchy")
      }
      scrollableObject.setGestureMargin(device.displayWidth / 10)
      scrollableObject.wait(Until.hasObject(By.textContains("Item 99")), 5_000)

      scrollableObject?.apply {
        repeat(2) { fling(Direction.DOWN) }
        repeat(2) { fling(Direction.UP) }
      }
      device.waitForIdle()
    }
  }
}
