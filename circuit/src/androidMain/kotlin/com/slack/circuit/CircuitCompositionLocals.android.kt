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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.slack.circuit.retained.LocalRetainedStateRegistryOwner
import com.slack.circuit.retained.continuityRetainedStateRegistry

@Composable
internal actual fun PlatformCompositionLocals(content: @Composable () -> Unit) {
  CompositionLocalProvider(
    LocalRetainedStateRegistryOwner provides continuityRetainedStateRegistry(),
  ) {
    content()
  }
}
