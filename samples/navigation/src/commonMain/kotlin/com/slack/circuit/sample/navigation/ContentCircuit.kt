package com.slack.circuit.sample.navigation

import com.slack.circuit.foundation.Circuit

fun buildCircuitForTabs(tabs: Collection<TabScreen>): Circuit {
  return Circuit.Builder()
    .apply {
      for (tab in tabs) {
        addPresenterFactory(TabPresenter.Factory(tab::class))
        addUiFactory(TabUiFactory(tab::class))
      }
    }
    .build()
}
