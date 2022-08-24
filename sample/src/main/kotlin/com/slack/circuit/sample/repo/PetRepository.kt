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
package com.slack.circuit.sample.repo

import com.slack.circuit.sample.data.Animal
import com.slack.circuit.sample.data.PetfinderApi
import javax.inject.Inject
import javax.inject.Singleton

interface PetRepository {
  suspend fun getAnimals(): List<Animal>
  suspend fun getAnimal(id: Long): Animal?
}

@Singleton
class PetRepositoryImpl @Inject constructor(private val petFinderApi: PetfinderApi) :
  PetRepository {
  private lateinit var animals: List<Animal>

  override suspend fun getAnimals(): List<Animal> {
    if (!this::animals.isInitialized) fetchAnimals()
    return animals
  }

  override suspend fun getAnimal(id: Long): Animal? {
    if (!this::animals.isInitialized) fetchAnimals()
    return animals.find { it.id == id }
  }

  private suspend fun fetchAnimals() {
    animals = petFinderApi.animals(limit = 100).animals
  }
}
