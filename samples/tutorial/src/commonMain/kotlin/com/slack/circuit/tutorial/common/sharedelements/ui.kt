// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial.common.sharedelements

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.tutorial.common.Email

/** A simple email item to show in a list. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EmailItem(email: Email, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
  SharedElementTransitionScope {
    Row(
      modifier.clickable(onClick = onClick).padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Image(
        Icons.Default.Person,
        modifier =
          Modifier.sharedElement(
              sharedContentState =
                rememberSharedContentState(
                  EmailSharedTransitionKey(
                    id = email.id,
                    type = EmailSharedTransitionKey.ElementType.SenderImage,
                  )
                ),
              animatedVisibilityScope =
                requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            )
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Magenta)
            .padding(4.dp),
        colorFilter = ColorFilter.tint(Color.White),
        contentDescription = null,
      )
      Column {
        Row {
          Text(
            text = email.sender,
            modifier =
              Modifier.sharedBounds(
                  sharedContentState =
                    rememberSharedContentState(
                      EmailSharedTransitionKey(
                        id = email.id,
                        type = EmailSharedTransitionKey.ElementType.SenderName,
                      )
                    ),
                  animatedVisibilityScope =
                    requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
                )
                .weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )

          Text(
            text = email.timestamp,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.5f),
          )
        }

        Text(
          text = email.subject,
          modifier =
            Modifier.sharedBounds(
              sharedContentState =
                rememberSharedContentState(
                  EmailSharedTransitionKey(
                    id = email.id,
                    type = EmailSharedTransitionKey.ElementType.Subject,
                  )
                ),
              animatedVisibilityScope =
                requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            ),
          style = MaterialTheme.typography.labelLarge,
        )
        Text(
          text = email.body,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyMedium,
          modifier =
            Modifier.sharedBounds(
                sharedContentState =
                  rememberSharedContentState(
                    EmailSharedTransitionKey(
                      id = email.id,
                      type = EmailSharedTransitionKey.ElementType.Body,
                    )
                  ),
                animatedVisibilityScope =
                  requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
              )
              .alpha(0.5f),
        )
      }
    }
  }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EmailDetailContent(email: Email, modifier: Modifier = Modifier) = SharedElementTransitionScope {
  Column(modifier.padding(16.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Image(
        Icons.Default.Person,
        modifier =
          Modifier.sharedElement(
              sharedContentState =
                rememberSharedContentState(
                  EmailSharedTransitionKey(
                    id = email.id,
                    type = EmailSharedTransitionKey.ElementType.SenderImage,
                  )
                ),
              animatedVisibilityScope =
                requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            )
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Magenta)
            .padding(4.dp),
        colorFilter = ColorFilter.tint(Color.White),
        contentDescription = null,
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
          Text(
            text = email.sender,
            modifier =
              Modifier.sharedBounds(
                  sharedContentState =
                    rememberSharedContentState(
                      EmailSharedTransitionKey(
                        id = email.id,
                        type = EmailSharedTransitionKey.ElementType.SenderName,
                      )
                    ),
                  animatedVisibilityScope =
                    requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
                )
                .weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = email.timestamp,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.5f),
          )
        }
        Text(
          text = email.subject,
          modifier =
            Modifier.sharedBounds(
              sharedContentState =
                rememberSharedContentState(
                  EmailSharedTransitionKey(
                    id = email.id,
                    type = EmailSharedTransitionKey.ElementType.Subject,
                  )
                ),
              animatedVisibilityScope =
                requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            ),
          style = MaterialTheme.typography.labelMedium,
        )
        Row {
          Text("To: ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          Text(
            text = email.recipients.joinToString(","),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.alpha(0.5f),
          )
        }
      }
    }
    @Suppress("DEPRECATION") // Deprecated in Android but not yet available in CM
    Divider(modifier = Modifier.padding(vertical = 16.dp))
    Text(
      text = email.body,
      Modifier.sharedBounds(
        sharedContentState =
          rememberSharedContentState(
            EmailSharedTransitionKey(
              id = email.id,
              type = EmailSharedTransitionKey.ElementType.Body,
            )
          ),
        animatedVisibilityScope =
          requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
      ),
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}
