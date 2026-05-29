// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sample.inbox.data.Email
import dev.zacsweers.metro.AppScope

/**
 * Controls what icon (if any) is rendered in the detail TopAppBar's navigation slot.
 * - [Back] — arrow-back (compact / standalone navigation use)
 * - [Close] — X (used in expanded two-pane layout to deselect)
 * - [None] — no nav icon at all
 */
enum class DetailNavStyle {
  Back,
  Close,
  None,
}

/**
 * Standalone-registered Circuit UI for [EmailDetailScreen]. Simple `(state, modifier)` signature
 * so the Circuit codegen / Metro graph can wire it up. Always renders the [DetailNavStyle.Back]
 * nav icon; the composite calls [EmailDetailPane] directly when it needs the deselect-style X.
 */
@CircuitInject(EmailDetailScreen::class, AppScope::class)
@Composable
fun EmailDetail(state: EmailDetailScreen.State, modifier: Modifier = Modifier) {
  EmailDetailPane(state, modifier, DetailNavStyle.Back)
}

/** Email detail pane with explicit nav-icon control. */
@Composable
fun EmailDetailPane(
  state: EmailDetailScreen.State,
  modifier: Modifier = Modifier,
  navStyle: DetailNavStyle = DetailNavStyle.Back,
) {
  when (state) {
    EmailDetailScreen.State.Loading -> LoadingPane(modifier)
    EmailDetailScreen.State.NotFound -> NotFoundPane(modifier)
    is EmailDetailScreen.State.Loaded -> LoadedPane(state, modifier, navStyle)
  }
}

@Composable
private fun LoadingPane(modifier: Modifier = Modifier) {
  DetailStatusPane(
    modifier = modifier,
    title = "Loading conversation",
    message = "Fetching the latest version.",
  ) {
    CircularProgressIndicator(modifier = Modifier.padding(20.dp).size(32.dp), strokeWidth = 3.dp)
  }
}

@Composable
private fun NotFoundPane(modifier: Modifier = Modifier) {
  DetailStatusPane(
    modifier = modifier,
    title = "Conversation unavailable",
    message = "This email is no longer available.",
  ) {
    Icon(
      imageVector = Icons.Filled.Email,
      contentDescription = null,
      modifier = Modifier.padding(20.dp).size(32.dp),
    )
  }
}

@Composable
private fun LoadedPane(
  state: EmailDetailScreen.State.Loaded,
  modifier: Modifier = Modifier,
  navStyle: DetailNavStyle = DetailNavStyle.Back,
) {
  val email = state.email
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = email.subject,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          when (navStyle) {
            DetailNavStyle.Back ->
              IconButton(onClick = { state.eventSink(EmailDetailScreen.Event.BackClicked) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            DetailNavStyle.Close ->
              IconButton(onClick = { state.eventSink(EmailDetailScreen.Event.BackClicked) }) {
                Icon(Icons.Filled.Close, contentDescription = "Deselect")
              }
            DetailNavStyle.None -> Unit
          }
        },
        actions = {
          if (email.isRead) {
            IconButton(onClick = { state.eventSink(EmailDetailScreen.Event.MarkUnread) }) {
              Icon(Icons.Filled.Email, contentDescription = "Mark unread")
            }
          }
          IconButton(onClick = { state.eventSink(EmailDetailScreen.Event.ToggleStar) }) {
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = if (email.isStarred) "Unstar" else "Star",
              tint =
                if (email.isStarred) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
          }
          IconButton(onClick = { state.eventSink(EmailDetailScreen.Event.Archive) }) {
            Icon(Icons.Filled.Delete, contentDescription = "Archive")
          }
        },
      )
    },
  ) { innerPadding ->
    EmailContent(
      email = email,
      modifier =
        Modifier.padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
    )
  }
}

@Composable
private fun EmailContent(email: Email, modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(text = email.subject, style = MaterialTheme.typography.headlineSmall)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = email.sender,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = email.timestamp,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.alpha(0.6f),
      )
    }
    Text(
      text = "to ${email.recipients.joinToString()}",
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.alpha(0.6f),
    )
    HorizontalDivider()
    Text(text = email.body, style = MaterialTheme.typography.bodyMedium)
  }
}

/** Placeholder shown in the detail pane when nothing is selected (expanded layout only). */
@Composable
fun EmptyDetailPane(modifier: Modifier = Modifier) {
  DetailStatusPane(
    modifier = modifier,
    title = "No conversation selected",
    message = "Pick an email from the list to read it here.",
  ) {
    Icon(
      imageVector = Icons.Filled.Email,
      contentDescription = null,
      modifier = Modifier.padding(20.dp).size(32.dp),
    )
  }
}

@Composable
private fun DetailStatusPane(
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  icon: @Composable () -> Unit,
) {
  // Subtly recessed surface so detail status panes read as part of the same list-detail surface
  // family rather than a bare background.
  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
          icon()
        }
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.widthIn(max = 280.dp),
        )
      }
    }
  }
}
