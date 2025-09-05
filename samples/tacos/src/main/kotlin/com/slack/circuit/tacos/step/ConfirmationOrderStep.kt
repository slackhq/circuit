// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.R
import com.slack.circuit.tacos.model.Diet
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.model.toCurrencyString

data object ConfirmationOrderStep : OrderStep {
  override val index = 2
  override val headerResId = R.string.confirm_step_header

  data class OrderState(
    val calories: Int,
    val baseCost: String,
    val ingredientsCost: String,
    val diet: Diet,
    val filling: String,
    val toppings: List<String>,
  ) : OrderStep.State
}

@Composable
internal fun confirmationProducer(orderDetails: OrderDetails): ConfirmationOrderStep.OrderState {
  val toppings = orderDetails.toppings.sortedBy { it.name }
  return ConfirmationOrderStep.OrderState(
    calories = calculateCalories(orderDetails.filling, orderDetails.toppings),
    baseCost = orderDetails.baseCost.toCurrencyString(),
    ingredientsCost = orderDetails.ingredientsCost.toCurrencyString(),
    diet = determineDiet(orderDetails.filling, orderDetails.toppings),
    filling = orderDetails.filling.name,
    toppings = toppings.map { it.name },
  )
}

private fun calculateCalories(filling: Ingredient, toppings: Set<Ingredient>): Int {
  val toppingsTotal = toppings.fold(0) { acc, topping -> acc + topping.calories }
  return toppingsTotal + filling.calories
}

private fun determineDiet(filling: Ingredient, toppings: Set<Ingredient>): Diet {
  val updater = { curDiet: Diet, ingredient: Ingredient ->
    when (ingredient.diet.ordinal < curDiet.ordinal) {
      true -> ingredient.diet
      false -> curDiet
    }
  }

  // Iterate over the list of toppings and determine the highest common dietary level
  // VEGAN being the highest, NONE being the lowest
  val diet = toppings.fold(Diet.entries.last(), updater)

  // Check if the filling impacts the dietary level...
  return updater(diet, filling)
}

@Composable
internal fun ConfirmationUi(
  state: ConfirmationOrderStep.OrderState,
  modifier: Modifier = Modifier,
) {
  with(state) {
    val summary = buildSummary(filling)
    val extra = buildExtras(diet, calories)

    Column(modifier = modifier.fillMaxSize().padding(4.dp)) {
      val scrollState = rememberScrollState()
      Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).weight(1f)) {
        Text(
          text = stringResource(R.string.confirm_step_details),
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(text = summary, modifier = Modifier.padding(bottom = 4.dp))
        toppings.forEachIndexed { i, topping ->
          Row(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = "${i + 1}.", modifier = Modifier.padding(end = 4.dp))
            Text(text = topping, fontWeight = FontWeight.Bold)
          }
        }
        Text(text = extra, modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(8.dp))
      }

      Box(modifier = Modifier.fillMaxWidth().padding(end = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth(fraction = 0.5f).align(Alignment.BottomEnd)) {
          Text(
            text = stringResource(R.string.confirm_step_charge_breakdown),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
          )

          Box(modifier = Modifier.align(Alignment.End).fillMaxWidth()) {
            Text(
              text = stringResource(R.string.confirm_step_taco),
              modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(text = "$$baseCost", modifier = Modifier.align(Alignment.CenterEnd))
          }

          Box(modifier = Modifier.align(Alignment.End).fillMaxWidth()) {
            Text(
              text = stringResource(R.string.confirm_step_extras),
              modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(text = "$$ingredientsCost", modifier = Modifier.align(Alignment.CenterEnd))
          }
        }
      }
    }
  }
}

private fun buildSummary(filling: String) = buildAnnotatedString {
  append("Your ")
  withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("$filling ") }
  append("taco will be topped with: ")
}

private fun buildExtras(diet: Diet, calories: Int) = buildAnnotatedString {
  append("This taco ")
  if (diet != Diet.NONE) {
    append("is ")
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
      append("${diet.name.lowercase()} ")
    }
    append("and ")
  }
  append("has approximately ")
  withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("$calories ") }
  append("calories.")
}
