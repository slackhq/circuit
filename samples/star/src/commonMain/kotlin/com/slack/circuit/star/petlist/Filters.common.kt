// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import com.slack.circuit.internal.runtime.Parcelable
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

@Parcelize
class Filters(
  val genders: ImmutableSet<Gender> = Gender.entries.asIterable().toImmutableSet(),
  val sizes: ImmutableSet<Size> = Size.entries.asIterable().toImmutableSet(),
) : Parcelable

/** Indirection for showing filters in the pet list screen. */
expect suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters
