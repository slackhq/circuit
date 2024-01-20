// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.repo

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.slack.circuit.star.data.PetfinderApi
import com.slack.circuit.star.db.Animal as DbAnimal
import com.slack.circuit.star.db.AnimalBio
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.ImmutableListAdapter
import com.slack.circuit.star.db.OpJournal
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.db.SqlDriverFactory
import com.slack.circuit.star.db.StarDatabase
import com.slack.eithernet.ApiResult
import com.slack.eithernet.retryWithExponentialBackoff
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class PetRepositoryImpl(
  sqliteDriverFactory: SqlDriverFactory,
  private val petFinderApi: PetfinderApi,
) : PetRepository {

  private val backgroundScope = CoroutineScope(SupervisorJob() + IO)
  private val driver = sqliteDriverFactory.create(StarDatabase.Schema, "star.db")
  private val starDb =
    StarDatabase(
      driver,
      DbAnimal.Adapter(
        ImmutableListAdapter(","),
        ImmutableListAdapter(":"),
        EnumColumnAdapter(),
        EnumColumnAdapter(),
      ),
    )

  override suspend fun refreshData() {
    withContext(IO) {
      fetchAnimals()
      // Clear the existing bios but lazily repopulate them
      starDb.starQueries.deleteAllBios()
    }
  }

  override fun animalsFlow(): Flow<List<DbAnimal>?> {
    backgroundScope.launch {
      if (isOperationStale("animals")) {
        // Fetch new data
        fetchAnimals()
      }
    }

    // If empty, check if the DB has been updated at all. If it hasn't, then it's just that we
    // haven't fetched yet and we return null instead to indicate it's still loading.
    return starDb.starQueries
      .getAllAnimals()
      .asFlow()
      .mapToList(backgroundScope.coroutineContext)
      .map { animals ->
        animals.ifEmpty {
          val dbHasOps = starDb.starQueries.lastUpdate("animals").executeAsOneOrNull()
          if (dbHasOps == null) {
            null
          } else {
            animals
          }
        }
      }
  }

  override suspend fun getAnimal(id: Long): DbAnimal? {
    return withContext(IO) { starDb.starQueries.getAnimal(id).executeAsOneOrNull() }
  }

  @Suppress("SwallowedException")
  private suspend fun fetchAnimals() {
    val result = retryWithExponentialBackoff {
      // Sometimes petfinder's API throws 429s for no reason.
      petFinderApi.animals(limit = 100)
    }
    if (result !is ApiResult.Success) {
      System.err.println("Failed to fetch animals: $result")
      return
    }
    val animals = result.value.animals
    // Do everything in a single transaction for atomicity
    starDb.transaction {
      // Delete any not present
      starDb.starQueries.deleteAllAnimals()

      // Re-populate the DB
      for ((index, animal) in animals.withIndex()) {
        starDb.starQueries.updateAnimal(
          DbAnimal(
            id = animal.id,
            sort = index.toLong(),
            // Names are sometimes all caps
            name =
              animal.name.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
              },
            url = animal.url,
            photoUrls = animal.photos.map { it.full }.toImmutableList(),
            primaryPhotoUrl = animal.photos.firstOrNull()?.medium,
            tags =
              listOfNotNull(
                  animal.colors.primary,
                  animal.colors.secondary,
                  animal.breeds.primary,
                  animal.breeds.secondary,
                  animal.gender,
                  animal.size,
                  animal.status,
                )
                .toImmutableList(),
            description = animal.description.orEmpty(),
            primaryBreed = animal.breeds.primary,
            gender = Gender.valueOf(animal.gender.uppercase()),
            size = Size.valueOf(animal.size.uppercase()),
            age = animal.age,
          )
        )
      }

      logUpdate("animals")
    }
  }

  override suspend fun getAnimalBio(id: Long): String? {
    return withContext(IO) {
      val animal = getAnimal(id) ?: return@withContext null
      val opId = "bio:$id"
      val isStale = isOperationStale(opId)
      val dbBio by lazy(NONE) { starDb.starQueries.getAnimalBio(id).executeAsOneOrNull() }
      if (isStale || dbBio == null) {
        when (val result = petFinderApi.animalBio(animal.url)) {
          is ApiResult.Success -> {
            val bio = result.value
            // Single transaction to log the operation update with the put
            starDb.transactionWithResult {
              logUpdate(opId)
              starDb.starQueries.putAnimalBio(AnimalBio(id, bio))
            }
            bio
          }
          is ApiResult.Failure -> null
        }
      } else {
        dbBio?.description
      }
    }
  }

  private fun logUpdate(operation: String) {
    val timestamp = currentTimestamp()
    starDb.starQueries.putUpdate(OpJournal(timestamp = timestamp, operation = operation))
  }

  /**
   * Returns whether or not it's been more than one day since the last update to the given
   * [operation].
   */
  private fun isOperationStale(operation: String): Boolean {
    val lastUpdate =
      starDb.starQueries.lastUpdate(operation).executeAsOneOrNull()?.timestamp ?: return true
    val timestamp = currentTimestamp()

    return (Instant.fromEpochSeconds(lastUpdate) - Instant.fromEpochSeconds(timestamp))
      .inWholeDays > 1
  }

  private fun currentTimestamp(): Long {
    return Clock.System.now().epochSeconds
  }
}
