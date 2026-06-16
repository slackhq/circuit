// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sample.inbox.data.EmailFolder
import com.slack.circuit.sample.inbox.detail.EmailDetailPresenter
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import com.slack.circuit.sample.inbox.list.InboxListPresenter
import com.slack.circuit.sample.inbox.list.InboxListScreen
import dev.zacsweers.metro.AppScope

/**
 * Composite presenter for [InboxScreen].
 *
 * The list and detail presenters are the same assisted classes Circuit registers for standalone
 * use. This presenter creates them with a [SelectionNavigator], which lets the children keep using
 * normal navigation calls while the parent decides how those calls map to local selection state.
 */
@CircuitInject(InboxScreen::class, AppScope::class)
@Composable
fun InboxPresenter(
  inboxListFactory: InboxListPresenter.Factory,
  emailDetailFactory: EmailDetailPresenter.Factory,
): InboxScreen.State {
  var selectedEmailId by rememberRetained { mutableStateOf<String?>(null) }

  val navigator = remember { SelectionNavigator { emailId -> selectedEmailId = emailId } }

  val listPresenter =
    remember(inboxListFactory, navigator) {
      inboxListFactory.create(InboxListScreen(EmailFolder.Inbox), navigator)
    }
  val listState = listPresenter.present()

  // Give each selected email its own retained detail state.
  val detailState = selectedEmailId?.let { id ->
    key(id) {
      val detailPresenter =
        remember(id, emailDetailFactory, navigator) {
          emailDetailFactory.create(EmailDetailScreen(id), navigator)
        }
      detailPresenter.present()
    }
  }

  return InboxScreen.State(
    listState = listState,
    detailState = detailState,
    selectedEmailId = selectedEmailId,
  ) { event ->
    when (event) {
      InboxScreen.Event.ClearSelection -> selectedEmailId = null
    }
  }
}

/** Adapts the child presenters' normal navigation events to local selection changes. */
private class SelectionNavigator(private val onSelectedEmailIdChanged: (String?) -> Unit) :
  Navigator by Navigator.NoOp {
  override fun goTo(screen: Screen): Boolean {
    if (screen !is EmailDetailScreen) return false

    onSelectedEmailIdChanged(screen.emailId)
    return true
  }

  override fun pop(result: PopResult?): Screen? {
    onSelectedEmailIdChanged(null)
    return null
  }
}
