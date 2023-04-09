// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.repo

import android.content.Context
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slack.circuit.star.Database
import com.slack.circuit.star.data.PetfinderApi
import com.slack.circuit.star.db.AnimalBio
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.GetAllAnimalsForList
import com.slack.circuit.star.db.GetAnimal
import com.slack.circuit.star.db.ImmutableListAdapter
import com.slack.circuit.star.db.OpJournal
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.ApplicationContext
import com.slack.circuit.star.di.SingleIn
import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE
import com.slack.circuit.star.db.Animal as DbAnimal

interface PetRepository {
  suspend fun refreshData()
  fun animalsFlow(): Flow<List<GetAllAnimalsForList>>
  suspend fun getAnimal(id: Long): GetAnimal?
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

  private val backgroundScope = CoroutineScope(Job() + Dispatchers.IO)
  private val driver = AndroidSqliteDriver(Database.Schema, appContext, "star.db")
  private val starDb =
    Database(
      driver,
      DbAnimal.Adapter(
        ImmutableListAdapter(","),
        ImmutableListAdapter(":"),
        EnumColumnAdapter(),
        EnumColumnAdapter(),
      )
    )

  override suspend fun refreshData() {
    fetchAnimals()
    // Clear the existing bios but lazily repopulate them
    starDb.starQueries.transaction { starDb.starQueries.deleteAllBios() }
  }

  override fun animalsFlow(): Flow<List<GetAllAnimalsForList>> {
    starDb.transaction {
      if (isOperationStale("animals")) {
        // Fetch new data
        backgroundScope.launch { fetchAnimals() }
      }
    }
    return starDb.starQueries
      .getAllAnimalsForList()
      .asFlow()
      .mapToList(backgroundScope.coroutineContext)
  }

  override suspend fun getAnimal(id: Long): GetAnimal? {
    return starDb.transactionWithResult { starDb.starQueries.getAnimal(id).executeAsOneOrNull() }
  }

  @Suppress("SwallowedException")
  private suspend fun fetchAnimals() {
    try {
      val animals = petFinderApi.animals(limit = 100).animals
      starDb.transaction {
        val allIds = animals.map { it.id }.toSet()
        val storedIds = starDb.starQueries.getAllIds().executeAsList().toSet()
        // Delete any not present
        for (id in storedIds - allIds) {
          starDb.starQueries.deleteAnimal(id)
        }

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
              description = animal.description,
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
    val animal = getAnimal(id) ?: return null
    val opId = "bio:$id"
    val isStale = isOperationStale(opId)
    val dbBio by lazy(NONE) { starDb.starQueries.getAnimalBio(id).executeAsOneOrNull() }
    return if (isStale || dbBio == null) {
      petFinderApi.animalBio(animal.url).also { bio ->
        starDb.transactionWithResult {
          logUpdate(opId)
          starDb.starQueries.putAnimalBio(AnimalBio(id, bio))
        }
      }
    } else {
      dbBio?.description
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
    val timestamp = currentTimestamp()
    val lastUpdate = starDb.starQueries.lastUpdate(operation).executeAsOneOrNull()?.timestamp
    return Duration.between(
        Instant.ofEpochSecond(timestamp),
        Instant.ofEpochSecond(lastUpdate ?: 0)
      )
      .toDays() > 1
  }

  private fun currentTimestamp(): Long {
    return Instant.now().atZone(ZoneOffset.UTC).toEpochSecond()
  }
}
