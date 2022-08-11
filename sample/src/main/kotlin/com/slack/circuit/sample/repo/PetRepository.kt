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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Singleton
class PetRepository @Inject constructor(private val petFinderApi: PetfinderApi) {
  private var animals: MutableStateFlow<List<Animal>> = MutableStateFlow(emptyList())
  private var fetched = false

  val animalsStateFlow: StateFlow<List<Animal>>
    get() {
      if (!fetched) {
        GlobalScope.launch { fetchAnimals() }
      }
      return animals
    }

  private suspend fun fetchAnimals() {
    val response = petFinderApi.animals(limit = 25)
    animals.tryEmit(response.animals)
    fetched = true
  }

  fun getAnimal(id: Long): Animal {
    val animalList = animalsStateFlow.value
    check(animalList.isNotEmpty()) {
      "Animal List is empty"
    }
    return animalList.find { it.id == id } ?: error("Cannot find animal $id in the list ")
  }
}
