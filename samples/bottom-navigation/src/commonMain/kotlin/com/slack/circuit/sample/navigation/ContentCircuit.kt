// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import com.slack.circuit.foundation.Circuit

fun buildCircuitForTabs(tabs: Collection<TabScreen>): Circuit {
  return Circuit.Builder()
    .apply {
      for (tab in tabs) {
        addPresenterFactory(TabPresenter.Factory(tab::class))
        addUiFactory(TabUiFactory(tab::class))
      }
      addPresenterFactory(DetailPresenter.Factory)
      addUiFactory(DetailUiFactory)
    }
    .setAnimatedNavDecoratorFactory(CrossFadeNavDecoratorFactory())
    .build()
}
