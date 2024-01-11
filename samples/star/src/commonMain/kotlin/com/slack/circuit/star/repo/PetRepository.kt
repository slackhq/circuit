// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.repo

import com.slack.circuit.star.db.Animal
import kotlinx.coroutines.flow.Flow

interface PetRepository {
  suspend fun refreshData()

  fun animalsFlow(): Flow<List<Animal>?>

  suspend fun getAnimal(id: Long): Animal?

  suspend fun getAnimalBio(id: Long): String?
}
