/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

/**
 * Renders the given [content] with the ability to show overlays on top of it. This works by
 * exposing an [OverlayHost] via [LocalOverlayHost].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param overlayHost the [OverlayHost] to use for managing overlays.
 * @param content The regular content to render. Any overlays will be rendered over them.
 */
@Composable
public fun ContentWithOverlays(
  modifier: Modifier = Modifier,
  overlayHost: OverlayHost = rememberOverlayHost(),
  content: @Composable () -> Unit
) {
  CompositionLocalProvider(LocalOverlayHost provides overlayHost) {
    val overlayHostData by rememberUpdatedState(overlayHost.currentOverlayData)
    Box(modifier) {
      content()
      key(overlayHostData) { overlayHostData?.let { data -> data.overlay.Content(data::finish) } }
    }
  }
}
