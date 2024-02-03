// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.parcel.CommonParcelable
import com.slack.circuit.star.parcel.CommonParcelize
import com.slack.circuit.star.parcel.CommonTypeParceler
import com.slack.circuit.star.parcel.ImmutableSetParceler
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

@CommonParcelize
class Filters(
  @CommonTypeParceler<ImmutableSet<Gender>, ImmutableSetParceler>
  val genders: ImmutableSet<Gender> = Gender.entries.asIterable().toImmutableSet(),
  @CommonTypeParceler<ImmutableSet<Size>, ImmutableSetParceler>
  val sizes: ImmutableSet<Size> = Size.entries.asIterable().toImmutableSet(),
) : CommonParcelable

/** Indirection for showing filters in the pet list screen. */
expect suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters
