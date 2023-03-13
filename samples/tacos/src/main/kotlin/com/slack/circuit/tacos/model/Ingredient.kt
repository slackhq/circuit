package com.slack.circuit.tacos.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
@Immutable
data class Ingredient(
  val name: String,
  val calories: Int = 0,
  val diet: Diet = Diet.NONE,
  val charge: BigDecimal = BigDecimal.ZERO
) : Parcelable

enum class Diet { VEGETARIAN, VEGAN, NONE }