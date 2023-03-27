// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.slack.circuit.tacos.R
import java.util.Locale

/** Order ingredient. Used to represent both fillings and toppings */
@Immutable
data class Ingredient(
  val name: String,
  val calories: Int = 0,
  val diet: Diet = Diet.NONE,
  val charge: Cents = 0
)

/** Dietary label. Used to mark an ingredient with the most strict diet that it satisfies. */
enum class Diet(@StringRes val codeResId: Int? = null) {
  NONE,
  VEGETARIAN(R.string.common_vegetarian),
  VEGAN(R.string.common_vegan)
}

typealias Cents = Int

fun Cents.toCurrencyString(): String = String.format(Locale.US, "%.2f", this / 100f)
