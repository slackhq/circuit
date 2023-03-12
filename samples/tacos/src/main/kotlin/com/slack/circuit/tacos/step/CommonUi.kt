package com.slack.circuit.tacos.step

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.model.Cents
import com.slack.circuit.tacos.model.Diet
import java.math.BigDecimal

@Composable
internal fun DietBadge(diet: Diet, modifier: Modifier = Modifier) {
  if (diet == Diet.NONE) return
  Text(diet.name.lowercase(), modifier)
}

@Composable
internal fun AdditionalCharge(charge: Cents, modifier: Modifier = Modifier) {
  if (charge <= 0) return
  val dollarAmount = BigDecimal(charge).movePointLeft(2)
  Text("$$dollarAmount", modifier)
}

@Composable
internal fun Calories(calories: Int, modifier: Modifier = Modifier) {
  if (calories <= 0) return
  Text("$calories cals", modifier)
}

@Composable
internal fun Spacer(charge: Cents, calories: Int, modifier: Modifier = Modifier) {
  if (charge <= 0 || calories <= 0) return
  Text(" | ", modifier)
}
