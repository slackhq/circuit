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
 * Composite presenter for [InboxScreen]. Combines a real [InboxListPresenter] and a real
 * [EmailDetailPresenter] into a single state.
 *
 * Both children are the exact same `Presenter` classes Circuit registers for standalone use
 * (they have `@CircuitInject` on their `@AssistedFactory`s). The composite gets both factories
 * injected by Metro and creates each child the same way Circuit's generated factory would — by
 * calling `factory.create(screen, navigator)` — and then calls `.present()` on the result.
 *
 * **How a child's navigation becomes selection state.** Each child dispatches "click an email"
 * (`navigator.goTo(EmailDetailScreen(id))`) and "back / archive" (`navigator.pop()`) through
 * the `Navigator` it was given — it has no idea whether it's standalone or embedded. Standalone,
 * that `Navigator` is the real one and those calls move the back stack. Embedded here, the
 * composite hands both children a single [SelectionNavigator]: a `Navigator` that performs no
 * real navigation and instead routes those two intents into this composite's own selection
 * state.
 *
 * `selectedEmailId` is held in `rememberRetained`, so it survives configuration changes —
 * including the device rotating between portrait and landscape (compact ↔ expanded layouts) or
 * a desktop window being resized. The UI swaps between single-pane and two-pane rendering, but
 * the user's selection stays put.
 */
@CircuitInject(InboxScreen::class, AppScope::class)
@Composable
fun InboxPresenter(
  inboxListFactory: InboxListPresenter.Factory,
  emailDetailFactory: EmailDetailPresenter.Factory,
): InboxScreen.State {
  var selectedEmailId by rememberRetained { mutableStateOf<String?>(null) }

  val navigator =
    remember {
      SelectionNavigator { emailId -> selectedEmailId = emailId }
    }

  val listPresenter =
    remember(inboxListFactory, navigator) {
      inboxListFactory.create(InboxListScreen(EmailFolder.Inbox), navigator)
    }
  val listState = listPresenter.present()

  // Detail presenter is recreated per selected id (via `key`) so its retained state is scoped
  // to that email.
  val detailState =
    selectedEmailId?.let { id ->
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

/**
 * A [Navigator] that does no real navigation. It exists so embedded child presenters can keep
 * speaking plain Circuit navigation (`navigator.goTo(...)` / `navigator.pop()`) while a
 * composite reinterprets those two intents:
 * - `goTo(EmailDetailScreen(id))` selects that email.
 * - `pop()` clears the selection.
 *
 * Everything else delegates to [Navigator.NoOp].
 */
private class SelectionNavigator(
  private val onSelectedEmailIdChanged: (String?) -> Unit,
) : Navigator by Navigator.NoOp {
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
