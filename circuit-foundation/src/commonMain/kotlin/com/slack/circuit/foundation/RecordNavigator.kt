// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.Navigator

/**
 * Overrides the [Navigator] given to the record composed in [content], in place of the
 * [NavigableCircuitContent] navigator. Answering navigators created by that record go through it
 * too.
 *
 * For [NavDecoration]s that show several records at once and need to know which one navigated. Keep
 * [navigator] the same instance for a record wherever it's composed, since presenters are keyed on
 * their navigator.
 */
@ExperimentalCircuitApi
@Composable
public fun ProvideRecordNavigator(navigator: Navigator, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalRecordNavigator provides navigator, content = content)
}

internal val LocalRecordNavigator: ProvidableCompositionLocal<Navigator?> = compositionLocalOf {
  null
}
