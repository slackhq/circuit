// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository

class TestIngredientsRepository(private val ingredients: List<Ingredient>) : IngredientsRepository {
  override suspend fun getFillings(): List<Ingredient> = ingredients

  override suspend fun getToppings(): List<Ingredient> = ingredients
}
