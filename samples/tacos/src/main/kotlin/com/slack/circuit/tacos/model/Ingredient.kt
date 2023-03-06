package com.slack.circuit.tacos.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

typealias Cents = Int

@Parcelize
@Immutable
data class Ingredient(
  val name: String,
  val calories: Int = 0,
  val diet: Diet = Diet.NONE,
  val charge: Cents = 0
) : Parcelable

enum class Diet { VEGETARIAN, VEGAN, NONE }