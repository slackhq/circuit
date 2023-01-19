// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Immutable
import com.slack.circuit.backstack.SaveableBackStack

/**
 * Represents an individual screen, used as a key for [Presenter.Factory] and [Ui.Factory].
 *
 * Screens can be simple sentinel `object` types or data classes with information to share. Screens
 * with information should contain the minimum amount of data needed for the target presenter to
 * begin presenting state.
 *
 * ```
 * data class AddFavorites(
 *   val externalId: UUID,
 * ) : Screen
 * ```
 *
 * Screens are then passed into [Navigators][Navigator] to navigate to them.
 *
 * ```
 * fun showAddFavorites() {
 *  navigator.goTo(
 *    AddFavorites(
 *      externalId = uuidGenerator.generate()
 *    )
 *  )
 * }
 * ```
 */
@Immutable public expect interface Screen

/**
 * Represents an individual screen with additional routing information.
 *
 * Instances of this interface are required when using [Navigator] with a [SaveableBackStack].
 *
 * [Routing][route] can be useful when constructing rich navigation and/or routing DSLs (eg. Deep
 * linking).
 *
 * @see Screen
 * @see Navigator
 * @see SaveableBackStack
 */
@Immutable
public expect interface NavigableScreen : Screen {
  public val route: String
}
