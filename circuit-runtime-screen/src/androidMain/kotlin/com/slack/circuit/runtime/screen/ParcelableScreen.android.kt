// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import android.os.Parcelable
import androidx.compose.runtime.Immutable

/**
 * Optional Android-only base type for screen keys that wish to support framework parcelization.
 *
 * If you are using `SaveableBackStack` on Android, implement this on your [Screen] type or annotate
 * with `@Parcelize` so that instances can be saved/restored by the framework.
 *
 * For cross-platform and process-safe persistence, prefer using `SerializableBackStack` with
 * kotlinx-serialization.
 */
@Immutable public interface ParcelableScreen : Screen, Parcelable
