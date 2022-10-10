/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
