package com.slack.circuit.tacos.step

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.tacos.R
import com.slack.circuit.tacos.model.Diet
import java.math.BigDecimal

private val fontSize = 8.sp

@Composable
internal fun Instructions(@StringRes resId: Int, modifier: Modifier = Modifier) {
  Text(
    text = stringResource(resId),
    modifier = modifier.padding(start = 15.dp, bottom = 10.dp),
    fontStyle = FontStyle.Italic
  )
}

@Composable
internal fun DietBadge(diet: Diet, modifier: Modifier = Modifier) {
  if (diet == Diet.NONE) return
  Box(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceTint, RoundedCornerShape(3.dp))
  ) {
    Text(
      text = diet.codeResId?.let { stringResource(it).uppercase() } ?: "",
      modifier = Modifier.padding(3.dp),
      fontSize = 6.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.surface
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
    text = stringResource(R.string.common_calories, calories),
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
