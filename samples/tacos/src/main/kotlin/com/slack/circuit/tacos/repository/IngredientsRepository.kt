// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.repository

import com.slack.circuit.tacos.model.Diet
import com.slack.circuit.tacos.model.Ingredient

interface IngredientsRepository {
  suspend fun getFillings(): List<Ingredient>

  suspend fun getToppings(): List<Ingredient>
}

object IngredientsRepositoryImpl : IngredientsRepository {
  override suspend fun getFillings(): List<Ingredient> {
    return fillings
  }

  override suspend fun getToppings(): List<Ingredient> {
    return toppings
  }
}

private val fillings =
  persistentListOf(
    Ingredient(name = "Chicken", calories = 180),
    Ingredient(name = "Steak", calories = 150, charge = 75),
    Ingredient(name = "Barbacoa", calories = 170, charge = 50),
    Ingredient(name = "Carnitas", calories = 170),
    Ingredient(name = "Tofu", calories = 150, diet = Diet.VEGAN),
    Ingredient(name = "Veggies", diet = Diet.VEGETARIAN, calories = 130),
  )

private val toppings =
  persistentListOf(
    Ingredient(name = "Guacamole", calories = 230, diet = Diet.VEGAN, charge = 295),
    Ingredient(name = "Queso Blanco", calories = 120, diet = Diet.VEGETARIAN, charge = 175),
    Ingredient(name = "Fajita Veggies", calories = 20, diet = Diet.VEGAN),
    Ingredient(name = "Tomato Salsa", calories = 25, diet = Diet.VEGAN),
    Ingredient(name = "Green Chilli Salsa", calories = 15, diet = Diet.VEGAN),
    Ingredient(name = "Sour Cream", calories = 110, diet = Diet.VEGETARIAN),
    Ingredient(name = "Cheese", calories = 110, diet = Diet.VEGETARIAN),
    Ingredient(name = "Black Beans", calories = 130, diet = Diet.VEGAN),
    Ingredient(name = "Pinto Beans", calories = 130, diet = Diet.VEGAN),
    Ingredient(name = "White Rice", calories = 210, diet = Diet.VEGAN),
    Ingredient(name = "Brown Rice", calories = 210, diet = Diet.VEGAN),
  )
