// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial.intro

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.tutorial.MainActivity
import com.slack.circuit.tutorial.common.EmailRepository

fun MainActivity.introTutorialOnCreate() {
  val emailRepository = EmailRepository()
  val circuit: Circuit =
    Circuit.Builder()
      .addPresenterFactory(DetailPresenter.Factory(emailRepository))
      .addPresenterFactory(InboxPresenter.Factory(emailRepository))
      .addUi<InboxScreen, InboxScreen.State> { state, modifier -> Inbox(state, modifier) }
      .addUi<DetailScreen, DetailScreen.State> { state, modifier -> EmailDetail(state, modifier) }
      .build()
  setContent {
    MaterialTheme {
      val backStack = rememberSaveableBackStack(InboxScreen)
      val navigator = rememberCircuitNavigator(backStack)
      CircuitCompositionLocals(circuit) {
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }
  }
}
