// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.tacos.R
import com.slack.circuit.tacos.model.Cents
import com.slack.circuit.tacos.model.Diet
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.model.toCurrencyString

private val fontSize = 8.sp

@Composable
internal fun Instructions(@StringRes resId: Int, modifier: Modifier = Modifier) {
  Text(
    text = stringResource(resId),
    modifier = modifier.padding(start = 16.dp, bottom = 8.dp),
    fontStyle = FontStyle.Italic,
  )
}

@Composable
internal fun OrderIngredient(ingredient: Ingredient) {
  Column {
    with(ingredient) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name)
        DietBadge(diet, Modifier.padding(4.dp))
      }
      Row {
        AdditionalCharge(charge)
        ExtrasDivider(charge, calories)
        Calories(calories)
      }
    }
  }
}

@Preview
@Composable
private fun PreviewOrderIngredient() {
  Surface {
    OrderIngredient(Ingredient(name = "Apple", calories = 99, diet = Diet.VEGAN, charge = 199))
  }
}

@Composable
private fun DietBadge(diet: Diet, modifier: Modifier = Modifier) {
  if (diet == Diet.NONE) return
  Box(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceTint, RoundedCornerShape(4.dp))
  ) {
    Text(
      text = diet.codeResId?.let { stringResource(it).uppercase() }.orEmpty(),
      modifier = Modifier.padding(4.dp),
      fontSize = 6.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.surface,
    )
  }
}

@Preview
@Composable
private fun PreviewDietBadge() {
  Surface { DietBadge(Diet.VEGAN) }
}

@Composable
private fun AdditionalCharge(charge: Cents, modifier: Modifier = Modifier) {
  if (charge <= 0) return
  Text(text = "$${charge.toCurrencyString()}", modifier = modifier, fontSize = fontSize)
}

@Preview
@Composable
@Suppress("MagicNumber")
private fun PreviewAdditionalCharge() {
  Surface { AdditionalCharge(125) }
}

/** Display calorie count. */
@Composable
private fun Calories(calories: Int, modifier: Modifier = Modifier) {
  if (calories <= 0) return
  Text(
    text = stringResource(R.string.common_calories, calories),
    modifier = modifier,
    fontSize = fontSize,
  )
}

@Preview
@Composable
@Suppress("MagicNumber")
private fun PreviewCalories() {
  Surface { Calories(150) }
}

@Composable
private fun ExtrasDivider(charge: Cents, calories: Int, modifier: Modifier = Modifier) {
  if (charge <= 0 || calories <= 0) return
  Text(text = " | ", modifier = modifier, fontSize = fontSize)
}
