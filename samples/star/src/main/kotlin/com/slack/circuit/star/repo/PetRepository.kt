// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.repo

import com.slack.circuit.star.data.Animal
import com.slack.circuit.star.data.PetfinderApi
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.SingleIn
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import retrofit2.HttpException

interface PetRepository {
  suspend fun getAnimals(forceRefresh: Boolean): List<Animal>
  suspend fun getAnimal(id: Long): Animal?
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PetRepositoryImpl @Inject constructor(private val petFinderApi: PetfinderApi) :
  PetRepository {
  private lateinit var animals: List<Animal>

  override suspend fun getAnimals(forceRefresh: Boolean): List<Animal> {
    if (!this::animals.isInitialized || forceRefresh) fetchAnimals()
    return animals
  }

  override suspend fun getAnimal(id: Long): Animal? {
    if (!this::animals.isInitialized) fetchAnimals()
    return animals.find { it.id == id }
  }

  @Suppress("SwallowedException")
  private suspend fun fetchAnimals() {
    animals =
      try {
        petFinderApi.animals(limit = 100).animals
      } catch (e: HttpException) {
        // Sometimes petfinder's API throws 429s for no reason.
        emptyList()
      }
  }
}
