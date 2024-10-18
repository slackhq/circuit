// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.ui.Modifier

/**
 * Conditionally applies the [transform] modifier based on the [predicate].
 *
 * Note: This is useful in cases where you want to apply a modifier like clickable in certain
 * situations but in others you don't want the View to react (ripple) to tap events.
 */
public inline fun Modifier.thenIf(
  predicate: Boolean,
  transform: Modifier.() -> Modifier,
): Modifier =
  if (predicate) {
    then(transform(Modifier))
  } else {
    this
  }

/**
 * Conditionally applies the [transform] modifier if the [value] is not null.
 *
 * Note: This is useful in cases where you want to apply a modifier like clickable in certain
 * situations but in others you don't want the View to react (ripple) to tap events.
 */
public inline fun <T> Modifier.thenIfNotNull(
  value: T?,
  transform: Modifier.(T) -> Modifier,
): Modifier {
  return if (value != null) {
    then(transform(Modifier, value))
  } else {
    this
  }
}
