package com.slack.circuit.tacos.repository

import com.slack.circuit.tacos.model.Diet
import com.slack.circuit.tacos.model.Ingredient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlin.random.Random

interface IngredientsRepository {
  suspend fun getFillings(): ImmutableList<Ingredient>
  suspend fun getToppings(): ImmutableList<Ingredient>
}

object IngredientsRepositoryImpl : IngredientsRepository {
  override suspend fun getFillings(): ImmutableList<Ingredient> {
    delay(Random.nextLong(0, 5000))
    return fillings
  }

  override suspend fun getToppings(): ImmutableList<Ingredient> {
    delay(Random.nextLong(0, 5000))
    return toppings
  }
}

// TODO move strings to resources
private val fillings = persistentListOf(
  Ingredient(
    name = "Chicken",
    calories = 180,
  ),
  Ingredient(
    name = "Steak",
    calories = 150,
    charge = 50,
  ),
  Ingredient(
    name = "Barbacoa",
    calories = 170,
    charge = 50,
  ),
  Ingredient(
    name = "Carnitas",
    calories = 170,
  ),
  Ingredient(
    name = "Tofu based",
    calories = 150,
    diet = Diet.VEGAN,
  ),
  Ingredient(
    name = "Veggies",
    diet = Diet.VEGETARIAN,
  ),
)

private val toppings = persistentListOf(
  Ingredient(
    name = "Guacamole",
    calories = 230,
    diet = Diet.VEGAN,
    charge = 295,
  ),
  Ingredient(
    name = "Queso Blanco",
    calories = 120,
    diet = Diet.VEGETARIAN,
    charge = 175,
  ),
  Ingredient(
    name = "Fajita Veggies",
    calories = 20,
    diet = Diet.VEGAN
  ),
  Ingredient(
    name = "Tomato Salsa",
    calories = 25,
    diet = Diet.VEGAN,
  ),
  Ingredient(
    name = "Green Chilli Salsa",
    calories = 15,
    diet = Diet.VEGAN,
  ),
  Ingredient(
    name = "Sour Cream",
    calories = 110,
    diet = Diet.VEGETARIAN,
  ),
  Ingredient(
    name = "Cheese",
    calories = 110,
    diet = Diet.VEGETARIAN,
  ),
  Ingredient(
    name = "Black Beans",
    calories = 130,
    diet = Diet.VEGAN,
  ),
  Ingredient(
    name = "Pinto Beans",
    calories = 130,
    diet = Diet.VEGAN,
  ),
  Ingredient(
    name = "White Rice",
    calories = 210,
    diet = Diet.VEGAN,
  ),
  Ingredient(
    name = "Brown Rice",
    calories = 210,
    diet = Diet.VEGAN,
  ),
)

