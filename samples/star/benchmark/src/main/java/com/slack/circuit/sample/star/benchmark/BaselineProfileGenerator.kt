// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.star.benchmark

import androidx.benchmark.macro.ExperimentalStableBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {
  @get:Rule val baselineProfileRule = BaselineProfileRule()

  @OptIn(ExperimentalStableBaselineProfilesApi::class)
  @Test
  fun startupBaselineProfile() =
    baselineProfileRule.collectStableBaselineProfile(
      packageName = "com.slack.circuit.sample.star.apk",
      // Iteration values recommended by AndroidX folks
      maxIterations = 15,
      stableIterations = 3,
      filterPredicate = { rule ->
        rule.contains("com/slack/circuit") &&
          !rule.contains("com/slack/circuit/sample") &&
          !rule.contains("com/slack/circuit/star")
      },
      profileBlock = { startActivityAndWait() }
    )
}
