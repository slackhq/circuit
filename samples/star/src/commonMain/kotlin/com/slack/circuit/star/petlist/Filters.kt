// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import com.slack.circuit.internal.runtime.Parcelable
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size

@Parcelize
class Filters(
  val genders: Set<Gender> = Gender.entries.toSet(),
  val sizes: Set<Size> = Size.entries.toSet(),
) : Parcelable

/** Indirection for showing filters in the pet list screen. */
expect suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters
