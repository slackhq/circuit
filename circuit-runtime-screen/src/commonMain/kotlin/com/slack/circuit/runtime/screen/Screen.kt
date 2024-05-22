// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import androidx.compose.runtime.Immutable

/**
 * Represents an individual screen, used as a key for `Presenter.Factory` and `Ui.Factory`.
 *
 * Screens can be simple sentinel `data object` types or data classes with information to share.
 * Screens with information should contain the minimum amount of data needed for the target
 * presenter to begin presenting state.
 *
 * ```
 * data class AddFavorites(
 *   val externalId: UUID,
 * ) : Screen
 * ```
 *
 * Screens are then passed into a `Navigator` to navigate to them.
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
 * A marker interface that indicates that this [Screen] only ever has static content and does not
 * require a presenter to compute or manage its state. This is useful for UIs that either have no
 * state or can derive their state solely from static inputs or the [Screen] key itself.
 *
 * **Stateless UI**
 *
 * ```kotlin
 * data object ProfileScreen : StaticScreen
 *
 * @Composable
 * fun Profile(modifier: Modifier = Modifier) {
 *   Text("Jane", modifier = modifier)
 * }
 *
 * val circuit = Circuit.Builder()
 *   .addUi<ProfileScreen> { _, modifier -> Profile(modifier) }
 *   .build()
 * ```
 *
 * **Static UI**
 *
 * ```kotlin
 * data object ProfileScreen : StaticScreen
 *
 * @Composable
 * fun Profile(name: String, modifier: Modifier = Modifier) {
 *   Text(name, modifier = modifier)
 * }
 *
 * val circuit = Circuit.Builder()
 *   .addUi<ProfileScreen> { _, modifier ->
 *     // The UI's input is handled in the factory in this case
 *     Profile("Jane", modifier)
 *   }
 *   .build()
 * ```
 *
 * **Static UI w/ Screen data**
 *
 * ```kotlin
 * data class ProfileScreen(val name: String) : StaticScreen
 *
 * @Composable
 * fun Profile(name: String, modifier: Modifier = Modifier) {
 *   Text(name, modifier = modifier)
 * }
 *
 * val circuit = Circuit.Builder()
 *   .addUi<ProfileScreen> { screen, modifier ->
 *     Profile(screen.name, modifier)
 *   }
 *   .build()
 * ```
 *
 * **Static UI w/ Screen input + code gen**
 *
 * ```kotlin
 * data class ProfileScreen(val name: String) : StaticScreen
 *
 * @CircuitInject(ProfileScreen::class, AppScope::class)
 * @Composable
 * fun Profile(screen: ProfileScreen, modifier: Modifier = Modifier) {
 *   Text(screen.name, modifier = modifier)
 * }
 * ```
 */
public expect interface StaticScreen : Screen
