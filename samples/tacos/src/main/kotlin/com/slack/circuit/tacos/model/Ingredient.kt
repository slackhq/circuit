package com.slack.circuit.tacos.model

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.slack.circuit.tacos.R
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

enum class Diet(@StringRes val codeResId: Int? = null) {
  NONE, VEGETARIAN(R.string.common_vegetarian), VEGAN(R.string.common_vegan)
}