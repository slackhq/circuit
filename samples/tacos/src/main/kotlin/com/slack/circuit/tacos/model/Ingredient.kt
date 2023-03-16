// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.slack.circuit.tacos.R
import java.math.BigDecimal

@Immutable
data class Ingredient(
  val name: String,
  val calories: Int = 0,
  val diet: Diet = Diet.NONE,
  val charge: BigDecimal = BigDecimal.ZERO
)

enum class Diet(@StringRes val codeResId: Int? = null) {
  NONE,
  VEGETARIAN(R.string.common_vegetarian),
  VEGAN(R.string.common_vegan)
}
