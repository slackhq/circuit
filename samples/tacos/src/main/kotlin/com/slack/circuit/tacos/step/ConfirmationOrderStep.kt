package com.slack.circuit.tacos.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
object ConfirmationOrderStep : OrderStep {
  @IgnoredOnParcel override val number = 2
  @IgnoredOnParcel override val headerResId = R.string.confirm_step_header

  data class Order(
    val calories: Int,
    val baseCost: BigDecimal,
    val ingredientsCost: BigDecimal,
    val diet: Diet,
    val filling: String,
    val toppings: ImmutableList<String>,
  ) : OrderStep.State
}

@Composable
internal fun confirmationProducer(orderDetails: OrderDetails): ConfirmationOrderStep.Order {
  val toppings = orderDetails.toppings.sortedBy { it.name }.toImmutableList()
  return ConfirmationOrderStep.Order(
    calories = calculateCalories(orderDetails.filling, orderDetails.toppings),
    baseCost = orderDetails.baseCost,
    ingredientsCost = orderDetails.ingredientsCost,
    diet = determineDiet(orderDetails.filling, orderDetails.toppings),
    filling = orderDetails.filling.name,
    toppings = toppings.map { it.name }.toImmutableList(),
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

  val diet = toppings.fold(Diet.values().last(), updater)
  return updater(diet, filling)
}

@Composable
internal fun ConfirmationUi(state: ConfirmationOrderStep.Order, modifier: Modifier = Modifier) {
  with(state) {
    val summary = buildSummary(filling)
    val extra = buildExtras(diet, calories)

    Column(modifier = modifier.padding(5.dp)) {
      Text(
        text = stringResource(R.string.confirm_step_details),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        modifier = Modifier.padding(bottom = 10.dp)
      )
      Text(
        text = summary,
        modifier = Modifier.padding(bottom = 5.dp)
      )
      toppings.forEachIndexed { i, topping ->
        Row(
          modifier = Modifier.padding(start = 10.dp)
        ) {
          Text(
            text = "${i+1}.",
            modifier = Modifier.padding(end = 5.dp)
          )
          Text(
            text = topping,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      Text(
        text = extra,
        modifier = Modifier.padding(top = 5.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(end = 5.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth(fraction = 0.5f)
            .align(Alignment.BottomEnd)
        ) {
          Text(
            text = stringResource(R.string.confirm_step_charge_breakdown),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 5.dp)
          )

          Box(
            modifier = Modifier
              .align(Alignment.End)
              .fillMaxWidth()
          ) {
            Text(
              text = stringResource(R.string.confirm_step_taco),
              modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
              text = "$$baseCost",
              modifier = Modifier.align(Alignment.CenterEnd)
            )
          }

          Box(
            modifier = Modifier
              .align(Alignment.End)
              .fillMaxWidth()
          ) {
            Text(
              text = stringResource(R.string.confirm_step_extras),
              modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
              text = "$$ingredientsCost",
              modifier = Modifier.align(Alignment.CenterEnd)
            )
          }
        }
      }
    }
  }
}

private fun buildSummary(filling: String) =
  buildAnnotatedString {
    append("Your ")
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
      append("$filling ")
    }
    append("taco will be topped with: ")
  }

private fun buildExtras(diet: Diet, calories: Int) =
  buildAnnotatedString {
    append("This taco ")
    if (diet != Diet.NONE) {
      append("is ")
      withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append("${diet.name.lowercase()} ")
      }
      append("and ")
    }
    append("has approximately ")
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
      append("$calories ")
    }
    append("calories.")
  }