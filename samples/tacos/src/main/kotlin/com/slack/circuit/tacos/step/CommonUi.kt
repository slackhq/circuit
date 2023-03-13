package com.slack.circuit.tacos.step

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.model.Diet
import java.math.BigDecimal

@Composable
internal fun DietBadge(diet: Diet, modifier: Modifier = Modifier) {
  if (diet == Diet.NONE) return
  Text(diet.name.lowercase(), modifier)
}

@Composable
internal fun AdditionalCharge(charge: BigDecimal, modifier: Modifier = Modifier) {
  if (charge <= BigDecimal.ZERO) return
  Text("$$charge", modifier)
}

@Composable
internal fun Calories(calories: Int, modifier: Modifier = Modifier) {
  if (calories <= 0) return
  Text("$calories cals", modifier)
}

@Composable
internal fun Spacer(charge: BigDecimal, calories: Int, modifier: Modifier = Modifier) {
  if (charge <= BigDecimal.ZERO || calories <= 0) return
  Text(" | ", modifier)
}
