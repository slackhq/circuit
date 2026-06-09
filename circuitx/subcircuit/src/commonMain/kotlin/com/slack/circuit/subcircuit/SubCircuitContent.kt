// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle

/**
 * Renders a SubCircuit screen by automatically resolving and connecting its [SubPresenter] and
 * [SubUi] components.
 *
 * This composable looks up the appropriate presenter and UI for the given [screen] from the
 * provided [subCircuit], then connects them together while passing events to the [outerEventSink].
 * If either the presenter or UI cannot be found, the [unavailableContent] will be shown instead.
 *
 * @param OuterEvent The type of events that will be delegated to the outer component.
 * @param screen The [SubScreen] to render.
 * @param outerEventSink A callback function for handling events that need to be processed by the
 *   outer Circuit or Composable (such as navigation events).
 * @param modifier The [Modifier] to apply to the rendered content.
 * @param subCircuit The [SubCircuit] instance to use for resolving the presenter and UI. Defaults
 *   to the current SubCircuit from composition locals.
 * @param unavailableContent A composable to show when the presenter or UI cannot be found. Defaults
 *   to a built-in error display.
 * @param contentKey An optional key for this content. Defaults to the [screen] instance. Change
 *   this to force granular recomposition when needed.
 * @see SubPresenter
 * @see SubScreen
 */
@Composable
public fun <OuterEvent : SubCircuitOuterEvent> SubCircuitContent(
  screen: SubScreen<OuterEvent>,
  outerEventSink: (OuterEvent) -> Unit,
  modifier: Modifier = Modifier,
  subCircuit: SubCircuit = requireNotNull(LocalSubCircuit.current) { "No SubCircuit provided!" },
  unavailableContent: @Composable (SubScreen<*>, Boolean, Boolean) -> Unit =
    DefaultUnavailableContent,
  contentKey: Any? = screen,
) {
  val presenter = remember(screen, subCircuit) { subCircuit.presenter(screen) }
  val ui = remember(screen, subCircuit) { subCircuit.ui(screen) }

  if (presenter != null && ui != null) {
    @Suppress("UNCHECKED_CAST")
    SubCircuitContent(
      screen = screen,
      presenter = presenter as SubPresenter<OuterEvent, SubCircuitUiState>,
      ui = ui as SubUi<SubCircuitUiState>,
      outerEventSink = outerEventSink,
      contentKey = contentKey,
      modifier = modifier,
    )
  } else {
    unavailableContent(screen, ui == null, presenter == null)
  }
}

/**
 * Renders a SubCircuit screen with explicitly provided [presenter] and [ui] components.
 *
 * This is a lower-level variant of [SubCircuitContent] that accepts pre-resolved presenter and UI
 * instances instead of looking them up from a SubCircuit. This is useful when you want direct
 * control over which components are used or for testing scenarios.
 *
 * @param OuterEvent The type of events that will be delegated to the outer component.
 * @param State The type of UI state produced by the presenter and consumed by the UI.
 * @param screen The [SubScreen] being rendered.
 * @param presenter The [SubPresenter] that will produce UI state.
 * @param ui The [SubUi] component that will render the state.
 * @param outerEventSink A callback function for handling events that need to be processed by the
 *   outer Circuit or Composable (such as navigation events).
 * @param modifier The [Modifier] to apply to the rendered content.
 * @param contentKey An optional key for this content. Defaults to the [screen] instance. Change
 *   this to force granular recomposition when needed.
 * @see SubPresenter
 * @see SubScreen
 */
@Composable
public fun <OuterEvent : SubCircuitOuterEvent, State : SubCircuitUiState> SubCircuitContent(
  screen: SubScreen<OuterEvent>,
  presenter: SubPresenter<OuterEvent, State>,
  ui: SubUi<State>,
  outerEventSink: (OuterEvent) -> Unit,
  modifier: Modifier = Modifier,
  contentKey: Any? = screen,
) {
  key(contentKey) {
    val state = presenter.present(outerEventSink)
    ui.Content(state, modifier)
  }
}

private val DefaultUnavailableContent: @Composable (SubScreen<*>, Boolean, Boolean) -> Unit =
  { screen, missingUi, missingPresenter ->
    if (LocalInspectionMode.current) {
      BasicText(
        text = "SubCircuitScreen(${screen::class.simpleName})",
        modifier = Modifier.background(Color.Gray),
        style = TextStyle(color = Color.Black),
      )
    } else {
      val text =
        when {
          missingUi && !missingPresenter -> "Ui not available for: $screen"
          !missingUi && missingPresenter -> "Presenter not available for: $screen"
          else -> "Route not available for: $screen"
        }
      BasicText(
        text = text,
        modifier = Modifier.background(Color.Red),
        style = TextStyle(color = Color.Yellow),
      )
    }
  }
