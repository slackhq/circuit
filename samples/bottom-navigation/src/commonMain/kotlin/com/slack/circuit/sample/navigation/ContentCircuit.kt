// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.animation.AnimatedSceneDecoration
import com.slack.circuit.runtime.ExperimentalCircuitApi

@OptIn(ExperimentalCircuitApi::class)
fun buildCircuitForTabs(tabs: Collection<TabScreen>): Circuit {
  return Circuit.Builder()
    .apply {
      for (tab in tabs) {
        addPresenterFactory(TabPresenter.Factory(tab::class))
        addUiFactory(TabUiFactory(tab::class))
      }
    }
    .setDefaultNavDecoration(AnimatedSceneDecoration())
    .build()
}
