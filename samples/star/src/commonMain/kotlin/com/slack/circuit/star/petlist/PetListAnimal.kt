// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.runtime.Immutable
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size

@Immutable
data class PetListAnimal(
  val id: Long,
  val name: String,
  val imageUrl: String?,
  val breed: String?,
  val gender: Gender?,
  val size: Size?,
  val age: String?,
  val imageAspectRatio: Float? = null,
)
