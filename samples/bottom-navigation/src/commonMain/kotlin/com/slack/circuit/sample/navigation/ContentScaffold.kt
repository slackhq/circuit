// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.runtime.Navigator
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ContentScaffold(
  backStack: SaveableBackStack,
  navigator: Navigator,
  tabs: ImmutableList<TabScreen>,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.testTag(ContentTags.TAG_SCAFFOLD).fillMaxSize(),
    bottomBar = { BottomTabRow(tabs, backStack, navigator) },
  ) { innerPadding ->
    NavigableCircuitContent(
      navigator = navigator,
      backStack = backStack,
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
    )
  }
}

@Composable
private fun BottomTabRow(
  tabs: ImmutableList<TabScreen>,
  backStack: SaveableBackStack,
  navigator: Navigator,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)),
  ) {
    tabs.forEach { tab ->
      val selected = tab == backStack.rootRecord?.screen
      Text(
        text = tab.label,
        color = if (selected) MaterialTheme.colorScheme.onSecondary else Color.Unspecified,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier =
          Modifier.testTag(ContentTags.TAG_TAB)
            .semantics { this.selected = selected }
            .weight(1f)
            .height(IntrinsicSize.Max)
            .clickable { navigator.resetRoot(tab, saveState = true, restoreState = true) }
            .background(if (selected) MaterialTheme.colorScheme.secondary else Color.Unspecified)
            .padding(horizontal = 8.dp, vertical = 20.dp),
      )
    }
    Icon(
      Icons.Default.Info,
      contentDescription = "Info",
      modifier =
        Modifier.clickable { navigator.goTo(InfoScreen) }
          .height(IntrinsicSize.Max)
          .minimumInteractiveComponentSize()
          .padding(horizontal = 8.dp, vertical = 20.dp),
    )
  }
}
