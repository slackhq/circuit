package com.slack.circuit.tacos.step

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.tacos.model.Diet
import java.math.BigDecimal

private val fontSize = 8.sp

@Composable
internal fun Instructions(instructions: String, modifier: Modifier = Modifier) {
  Text(
    text = instructions,
    modifier = modifier.padding(start = 15.dp, bottom = 10.dp),
    fontStyle = FontStyle.Italic
  )
}

@Composable
internal fun DietBadge(diet: Diet, modifier: Modifier = Modifier) {
  if (diet == Diet.NONE) return
  Box(
    modifier = modifier.background(Color.Gray, shape = RoundedCornerShape(3.dp))
  ) {
    Text(
      text = diet.code.uppercase(),
      modifier = Modifier.padding(3.dp),
      fontSize = 6.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White
    )
  }
}

@Composable
internal fun AdditionalCharge(charge: BigDecimal, modifier: Modifier = Modifier) {
  if (charge <= BigDecimal.ZERO) return
  Text(
    text = "$$charge",
    modifier = modifier,
    fontSize = fontSize
  )
}

@Composable
internal fun Calories(calories: Int, modifier: Modifier = Modifier) {
  if (calories <= 0) return
  Text(
    text = "$calories cals",
    modifier = modifier,
    fontSize = fontSize
  )
}

@Composable
internal fun ExtrasDivider(charge: BigDecimal, calories: Int, modifier: Modifier = Modifier) {
  if (charge <= BigDecimal.ZERO || calories <= 0) return
  Text(
    text = " | ",
    modifier = modifier,
    fontSize = 8.sp
  )
}

@Preview
@Composable
internal fun PreviewAdditionalCharge() {
  Surface { AdditionalCharge(BigDecimal("1.25")) }
}

@Preview
@Composable
internal fun PreviewDietBadge() {
  Surface { DietBadge(Diet.VEGAN) }
}

@Preview
@Composable
internal fun PreviewCalories() {
  Surface { Calories(150) }
}
