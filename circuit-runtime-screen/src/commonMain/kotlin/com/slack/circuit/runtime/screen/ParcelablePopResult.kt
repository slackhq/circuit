// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import androidx.compose.runtime.Immutable

/**
 * A [PopResult] that is also `android.os.Parcelable` on Android.
 *
 * See [ParcelableScreen] for when to use this.
 */
@Immutable public expect interface ParcelablePopResult : PopResult