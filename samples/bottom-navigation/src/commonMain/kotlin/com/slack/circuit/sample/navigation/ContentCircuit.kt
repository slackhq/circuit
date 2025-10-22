// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import com.slack.circuit.foundation.Circuit
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory

fun buildCircuitForTabs(tabs: Collection<TabScreen>): Circuit {
  return Circuit.Builder()
    .apply {
      addPresenterFactory(ContentPresenter.Factory)
      addUiFactory(ContentUiFactory)
      for (tab in tabs) {
        addPresenterFactory(TabPresenter.Factory(tab::class))
        addUiFactory(TabUiFactory(tab::class))
      }
      addPresenterFactory(DetailPresenter.Factory)
      addUiFactory(DetailUiFactory)
    }
    .setAnimatedNavDecoratorFactory(GestureNavigationDecorationFactory())
    .build()
}
