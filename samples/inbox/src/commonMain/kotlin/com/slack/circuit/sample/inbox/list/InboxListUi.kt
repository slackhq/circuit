// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sample.inbox.data.Email
import com.slack.circuit.sample.inbox.data.EmailFolder
import dev.zacsweers.metro.AppScope

/** Circuit-registered UI for the standalone [InboxListScreen]. */
@CircuitInject(InboxListScreen::class, AppScope::class)
@Composable
fun InboxList(state: InboxListScreen.State, modifier: Modifier = Modifier) {
  InboxListPane(state = state, modifier = modifier)
}

/** Shared list pane used by both the standalone list screen and the composite inbox screen. */
@Composable
fun InboxListPane(
  state: InboxListScreen.State,
  modifier: Modifier = Modifier,
  selectedEmailId: String? = null,
  scrollState: LazyListState = rememberLazyListState(),
) {
  Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Inbox") }) }) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding)) {
      FolderTabs(
        folders = state.folders,
        selected = state.folder,
        onSelected = { state.eventSink(InboxListScreen.Event.FolderChanged(it)) },
      )
      HorizontalDivider()
      LazyColumn(state = scrollState) {
        items(items = state.emails, key = { it.id }) { email ->
          // animateItem() fades new rows in, fades removed rows out (e.g. archiving an email),
          // and animates the remaining rows into their new positions. Requires the stable
          // `key` above.
          Column(modifier = Modifier.animateItem()) {
            EmailRow(
              email = email,
              selected = email.id == selectedEmailId,
              onClick = { state.eventSink(InboxListScreen.Event.EmailClicked(email.id)) },
              onToggleStar = { state.eventSink(InboxListScreen.Event.ToggleStar(email.id)) },
            )
            HorizontalDivider(modifier = Modifier.alpha(0.3f))
          }
        }
      }
    }
  }
}

@Composable
private fun FolderTabs(
  folders: List<EmailFolder>,
  selected: EmailFolder,
  onSelected: (EmailFolder) -> Unit,
) {
  PrimaryTabRow(selectedTabIndex = folders.indexOf(selected)) {
    folders.forEach { folder ->
      Tab(
        selected = folder == selected,
        onClick = { onSelected(folder) },
        text = { Text(folder.displayName) },
      )
    }
  }
}

@Composable
private fun EmailRow(
  email: Email,
  selected: Boolean,
  onClick: () -> Unit,
  onToggleStar: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
  Row(
    modifier
      .background(background)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    SenderAvatar(email.sender)
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = email.sender,
          modifier = Modifier.weight(1f),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = email.timestamp,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.alpha(0.6f),
        )
      }
      Text(
        text = email.subject,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = email.snippet,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.alpha(0.6f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    IconButton(onClick = onToggleStar, modifier = Modifier.size(32.dp)) {
      Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = if (email.isStarred) "Unstar" else "Star",
        tint =
          if (email.isStarred) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
      )
    }
  }
}

private val avatarPalette =
  listOf(
    Color(0xFF6750A4),
    Color(0xFF7D5260),
    Color(0xFF625B71),
    Color(0xFF8C4A60),
    Color(0xFF386A20),
    Color(0xFF4A6363),
  )

@Composable
private fun SenderAvatar(sender: String, modifier: Modifier = Modifier) {
  val idx = ((sender.hashCode() % avatarPalette.size) + avatarPalette.size) % avatarPalette.size
  Box(
    modifier.size(40.dp).clip(CircleShape).background(avatarPalette[idx]).padding(4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Default.Person,
      modifier = Modifier.size(28.dp),
      contentDescription = null,
      tint = Color.White,
    )
  }
}
