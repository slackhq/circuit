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

import android.os.Parcelable

/**
 * Represents an individual screen, used as a key for [PresenterFactory] and [UiFactory].
 *
 * Screens can be simple sentinel `object` types or data classes with information to share. Screens
 * with information should contain the minimum amount of data needed for the target presenter to
 * begin presenting state.
 *
 * ```
 * @Parcelize
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
interface Screen : Parcelable
