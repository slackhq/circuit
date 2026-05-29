// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sample.inbox.detail.DetailNavStyle
import com.slack.circuit.sample.inbox.detail.EmailDetailPane
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import com.slack.circuit.sample.inbox.detail.EmptyDetailPane
import com.slack.circuit.sample.inbox.list.InboxListPane
import com.slack.circuit.sample.inbox.list.InboxListScreen
import com.slack.circuit.sample.inbox.list.LocalSelectedEmailId
import dev.zacsweers.metro.AppScope

/**
 * Adaptive UI for the composite [InboxScreen].
 *
 * Window size is handled here instead of in the presenter: compact widths render one pane, while
 * expanded widths render the list and detail side by side. The compact list-detail transition is
 * still driven by Circuit's [NavDecoration] so it behaves like regular navigation.
 */
@CircuitInject(InboxScreen::class, AppScope::class)
@Composable
fun InboxHomeUi(state: InboxScreen.State, modifier: Modifier = Modifier) {
  val isExpanded = calculateWindowSizeClass().widthSizeClass == WindowWidthSizeClass.Expanded

  // Keep the same scroll state when the layout moves between one pane and two panes.
  val listScrollState = rememberRetainedSaveable(saver = LazyListState.Saver) { LazyListState() }

  CompositionLocalProvider(LocalSelectedEmailId provides state.selectedEmailId) {
    AnimatedContent(
      targetState = isExpanded,
      modifier = modifier,
      transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
      label = "InboxLayout",
    ) { expanded ->
      if (expanded) {
        Row(Modifier.fillMaxSize()) {
          InboxListPane(
            state = state.listState,
            modifier = Modifier.width(360.dp).fillMaxHeight(),
            scrollState = listScrollState,
          )
          VerticalDivider()
          val detail = state.detailState
          if (detail != null) {
            EmailDetailPane(
              state = detail,
              modifier = Modifier.fillMaxSize(),
              navStyle = DetailNavStyle.Close,
            )
          } else {
            EmptyDetailPane(modifier = Modifier.fillMaxSize())
          }
        }
      } else {
        CompactSinglePane(state = state, listScrollState = listScrollState)
      }
    }
  }
}

@Composable
private fun CompactSinglePane(state: InboxScreen.State, listScrollState: LazyListState) {
  BackHandlerForSelection(active = state.detailState != null) {
    state.eventSink(InboxScreen.Event.ClearSelection)
  }

  // Model the compact pane swap as a tiny back stack so NavDecoration can animate it.
  val args: NavStackList<InboxPane> =
    remember(state.selectedEmailId) {
      val id = state.selectedEmailId
      if (id == null) navStackListOf(InboxPane.List)
      else
        navStackListOf(
          forwardItems = emptyList(),
          activeItem = InboxPane.Detail(id),
          backwardItems = listOf(InboxPane.List),
        )
    }

  // Previews may render without a Circuit in the composition.
  val decoration: NavDecoration = LocalCircuit.current?.defaultNavDecoration ?: FadeNavDecoration

  decoration.DecoratedContent(args = args, modifier = Modifier.fillMaxSize()) { pane ->
    when (pane) {
      InboxPane.List ->
        InboxListPane(
          state = state.listState,
          modifier = Modifier.fillMaxSize(),
          scrollState = listScrollState,
        )
      is InboxPane.Detail ->
        // A pop animation can outlive the selected detail state.
        state.detailState?.let { detail ->
          EmailDetailPane(
            state = detail,
            modifier = Modifier.fillMaxSize(),
            navStyle = DetailNavStyle.Back,
          )
        }
    }
  }
}

/** Synthetic compact-mode destinations used only for transition identity. */
private sealed interface InboxPane : NavArgument {
  data object List : InboxPane {
    override val key: String = "inbox-list"
    override val screen: Screen = InboxListScreen()
  }

  data class Detail(val emailId: String) : InboxPane {
    override val key: String = "email-detail-$emailId"
    override val screen: Screen = EmailDetailScreen(emailId)
  }
}

/** Fallback decoration for previews and tests that do not provide a Circuit. */
private object FadeNavDecoration : NavDecoration {
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    AnimatedContent(
      targetState = args.active,
      modifier = modifier,
      transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
      contentKey = { it.key },
      label = "FadeNavDecoration",
    ) {
      content(it)
    }
  }
}

/** Platform hook for clearing selection from system back. */
@Composable expect fun BackHandlerForSelection(active: Boolean, onBack: () -> Unit)
