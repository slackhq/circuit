// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.repo

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.slack.circuit.star.data.socialteesnyc.StarApi
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.ListColumnAdapter
import com.slack.circuit.star.db.OpJournal
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.db.SqlDriverFactory
import com.slack.circuit.star.db.StarDatabase
import com.slack.eithernet.ApiResult
import com.slack.eithernet.exceptionOrNull
import com.slack.eithernet.retryWithExponentialBackoff
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val METADATA_KEY_UPDATED_AT = "animals_updated_at"

interface PetRepository {
  suspend fun refreshData()

  fun animalsFlow(): Flow<List<Animal>?>

  suspend fun getAnimal(id: Long): Animal?

  suspend fun getAnimalBio(id: Long): String?
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class PetRepositoryImpl(sqliteDriverFactory: SqlDriverFactory, private val starApi: StarApi) :
  PetRepository {

  private val backgroundScope = CoroutineScope(SupervisorJob() + IO)
  private val driver = sqliteDriverFactory.create(StarDatabase.Schema, "star.db")
  private val starDb =
    StarDatabase(
      driver,
      Animal.Adapter(
        // Use | as delimiter since URLs contain commas
        ListColumnAdapter("|"),
        ListColumnAdapter(":"),
        EnumColumnAdapter(),
        EnumColumnAdapter(),
      ),
    )

  override suspend fun refreshData() {
    withContext(IO) { fetchAnimals(forceRefresh = true) }
  }

  override fun animalsFlow(): Flow<List<Animal>?> {
    backgroundScope.launch {
      if (isOperationStale("animals")) {
        // Fetch new data
        fetchAnimals(forceRefresh = false)
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

  override suspend fun getAnimal(id: Long): Animal? {
    return withContext(IO) { starDb.starQueries.getAnimal(id).executeAsOneOrNull() }
  }

  @Suppress("SwallowedException")
  private suspend fun fetchAnimals(forceRefresh: Boolean) {
    val result = retryWithExponentialBackoff { starApi.getPets() }
    if (result !is ApiResult.Success) {
      System.err.println("Failed to fetch animals: $result.")
      (result as ApiResult.Failure).exceptionOrNull()?.stackTraceToString()?.let {
        System.err.println("Trace is:\n$it")
      }
      return
    }

    val response = result.value
    val newUpdatedAt = response.updatedAt

    // Check if the data has changed since last fetch (unless force refreshing)
    if (!forceRefresh) {
      val lastUpdatedAtEpoch =
        starDb.starQueries.getMetadata(METADATA_KEY_UPDATED_AT).executeAsOneOrNull()?.toLongOrNull()
      if (lastUpdatedAtEpoch != null) {
        val lastUpdatedAt = Instant.fromEpochSeconds(lastUpdatedAtEpoch)
        if (newUpdatedAt <= lastUpdatedAt) {
          // Data hasn't changed, just update the operation timestamp so we don't check again soon
          logUpdate("animals")
          return
        }
      }
    }

    val animals = response.pets
    // Do everything in a single transaction for atomicity
    starDb.transaction {
      // Delete any not present
      starDb.starQueries.deleteAllAnimals()

      // Re-populate the DB
      for ((index, pet) in animals.withIndex()) {
        starDb.starQueries.updateAnimal(
          Animal(
            id = pet.id.toLong(),
            sort = index.toLong(),
            // Names are sometimes all caps
            name =
              pet.name.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
              },
            url = pet.url,
            photoUrls = listOfNotNull(pet.photoUrl),
            primaryPhotoUrl = pet.photoUrl,
            tags = listOfNotNull(pet.petType, pet.breed, pet.sex, pet.size),
            description = pet.description,
            primaryBreed = pet.breed,
            gender = Gender.fromApiString(pet.sex),
            size = Size.fromApiString(pet.size),
            age = pet.age,
          )
        )
      }

      // Store the API's updatedAt timestamp as epoch seconds
      starDb.starQueries.putMetadata(METADATA_KEY_UPDATED_AT, newUpdatedAt.epochSeconds.toString())

      logUpdate("animals")
    }
  }

  override suspend fun getAnimalBio(id: Long): String? {
    // The SocialTees API already provides full descriptions, no need to fetch separately
    return withContext(IO) { getAnimal(id)?.description }
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

    return (Instant.fromEpochSeconds(timestamp) - Instant.fromEpochSeconds(lastUpdate))
      // Data updates 2x/day
      .inWholeHours > 12
  }

  private fun currentTimestamp(): Long {
    return Clock.System.now().epochSeconds
  }
}
