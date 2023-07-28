// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui

private object ValidationMarker

/** Returns true if this [CircuitContext] is currently validating via [CircuitConfig.validate]. */
public var CircuitContext.isValidating: Boolean
  get() = tag<ValidationMarker>() != null
  private set(value) {
    if (value) {
      putTag(ValidationMarker)
    } else {
      putTag<ValidationMarker>(null)
    }
  }

/**
 * Validates that the given [screen] has a matching [Ui.Factory] and [Presenter.Factory] in this
 * [CircuitConfig].
 */
public fun CircuitConfig.validate(
  screen: Screen,
  navigator: Navigator = Navigator.NoOp
): ValidationResult {
  return validate(listOf(screen), navigator)
}

/**
 * Validates that the given [screens] all have a matching [Ui.Factory] and [Presenter.Factory] in
 * this [CircuitConfig].
 *
 * @return a [ValidationResult] with information about any validation failures.
 */
public fun CircuitConfig.validate(
  screens: List<Screen>,
  navigator: Navigator = Navigator.NoOp
): ValidationResult {
  val screensMissingPresenters = mutableListOf<Screen>()
  val screensMissingUi = mutableListOf<Screen>()
  val context = newContext()
  context.isValidating = true

  for (screen in screens) {
    if (presenter(screen, navigator, context) == null) {
      screensMissingPresenters.add(screen)
    }
    if (ui(screen, context) == null) {
      screensMissingUi.add(screen)
    }
  }

  return if (screensMissingPresenters.isEmpty() && screensMissingUi.isEmpty()) {
    ValidationResult.Success
  } else {
    ValidationResult.Failure(screensMissingPresenters, screensMissingUi)
  }
}

/**
 * Sealed type hierarchy representing the result of a [CircuitConfig.validate] call.
 *
 * [Success] indicates that all [Screens][Screen] passed validation. [Failure] indicates that some
 * [Screens][Screen] failed validation and offers information about about which ones failed.
 */
public interface ValidationResult {
  public object Success : ValidationResult

  public data class Failure(
    public val screensMissingPresenters: List<Screen>,
    public val screensMissingUi: List<Screen>,
  ) : ValidationResult
}
