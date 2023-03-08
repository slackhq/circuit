package com.slack.circuit.tacos.step

import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository
import kotlinx.collections.immutable.ImmutableList

class TestIngredientsRepository(private val ingredients: ImmutableList<Ingredient>) : IngredientsRepository {
  override suspend fun getFillings(): ImmutableList<Ingredient> = ingredients
  override suspend fun getToppings(): ImmutableList<Ingredient> = ingredients
}
