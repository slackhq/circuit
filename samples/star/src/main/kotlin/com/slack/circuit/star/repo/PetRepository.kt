// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.repo

import android.content.Context
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slack.circuit.star.data.PetfinderApi
import com.slack.circuit.star.db.Animal as DbAnimal
import com.slack.circuit.star.db.AnimalBio
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.ImmutableListAdapter
import com.slack.circuit.star.db.OpJournal
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.db.StarDatabase
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.ApplicationContext
import com.slack.circuit.star.di.SingleIn
import com.squareup.anvil.annotations.ContributesBinding
import java.time.Duration
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface PetRepository {
  suspend fun refreshData()

  fun animalsFlow(): Flow<List<DbAnimal>?>

  suspend fun getAnimal(id: Long): DbAnimal?

  suspend fun getAnimalBio(id: Long): String?
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PetRepositoryImpl
@Inject
constructor(
  @ApplicationContext private val appContext: Context,
  private val petFinderApi: PetfinderApi,
) : PetRepository {

  private val backgroundScope = CoroutineScope(SupervisorJob() + IO)
  private val driver = AndroidSqliteDriver(StarDatabase.Schema, appContext, "star.db")
  private val starDb =
    StarDatabase(
      driver,
      DbAnimal.Adapter(
        ImmutableListAdapter(","),
        ImmutableListAdapter(":"),
        EnumColumnAdapter(),
        EnumColumnAdapter(),
      )
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
    try {
      val animals = petFinderApi.animals(limit = 100).animals
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
                  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
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
                    animal.status
                  )
                  .toImmutableList(),
              description = animal.description.orEmpty(),
              primaryBreed = animal.breeds.primary,
              gender = Gender.valueOf(animal.gender.uppercase(Locale.US)),
              size = Size.valueOf(animal.size.uppercase(Locale.US)),
              age = animal.age,
            )
          )
        }

        logUpdate("animals")
      }
    } catch (e: HttpException) {
      // Sometimes petfinder's API throws 429s for no reason.
      // TODO retry?
    }
  }

  override suspend fun getAnimalBio(id: Long): String? {
    return withContext(IO) {
      val animal = getAnimal(id) ?: return@withContext null
      val opId = "bio:$id"
      val isStale = isOperationStale(opId)
      val dbBio by lazy(NONE) { starDb.starQueries.getAnimalBio(id).executeAsOneOrNull() }
      if (isStale || dbBio == null) {
        try {
          petFinderApi.animalBio(animal.url).also {
            // Single transaction to log the operation update with the put
            starDb.transactionWithResult {
              logUpdate(opId)
              starDb.starQueries.putAnimalBio(AnimalBio(id, it))
            }
          }
        } catch (e: HttpException) {
          null
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
    return Duration.between(Instant.ofEpochSecond(timestamp), Instant.ofEpochSecond(lastUpdate))
      .toDays() > 1
  }

  private fun currentTimestamp(): Long {
    return Instant.now().epochSecond
  }
}
