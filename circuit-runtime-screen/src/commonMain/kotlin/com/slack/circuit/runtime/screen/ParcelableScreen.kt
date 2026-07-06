// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import androidx.compose.runtime.Immutable

/**
 * A [Screen] that is also `android.os.Parcelable` on Android.
 *
 * Use this for common-code screens that persist via Parcelable on android, since common code
 * cannot reference `android.os.Parcelable` directly. On all other platforms this is just a
 * [Screen].
 *
 * ```
 * @Parcelize // Or your own `@CommonParcelize` annotation
 * data class AddFavorites(val externalId: UUID) : ParcelableScreen
 * ```
 */
@Immutable public expect interface ParcelableScreen : Screen
