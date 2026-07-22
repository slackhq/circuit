// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import com.slack.circuit.star.benchmark.ListBenchmarksItemScreen
import com.slack.circuit.star.benchmark.ListBenchmarksScreen
import com.slack.circuit.star.db.Gender.FEMALE
import com.slack.circuit.star.db.Size.LARGE
import com.slack.circuit.star.home.AboutScreen
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.imageviewer.ImageViewerScreen
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.petdetail.PetPhotoCarouselScreen
import com.slack.circuit.star.petlist.Filters
import com.slack.circuit.star.petlist.FiltersScreen
import com.slack.circuit.star.petlist.PetListScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StarCircuitSaverTest : BasePresenterTest() {

  @Test
  fun allCircuitSaveablesRoundTrip() {
    val filters = Filters(genders = setOf(FEMALE), sizes = setOf(LARGE))
    val partialAnimal =
      PetDetailScreen.PartialAnimal(
        id = 42,
        name = "Puppy",
        imageUrl = "https://example.com/puppy.jpg",
        breed = "Mutt",
        gender = FEMALE,
        size = LARGE,
      )
    assertScreenRoundTrips(ListBenchmarksScreen(useNestedContent = true))
    assertScreenRoundTrips(ListBenchmarksItemScreen(index = 7))
    assertScreenRoundTrips(AboutScreen)
    assertScreenRoundTrips(HomeScreen)
    assertScreenRoundTrips(
      ImageViewerScreen(
        id = 42,
        url = "https://example.com/puppy.jpg",
        index = 1,
        placeholderKey = "puppy",
      )
    )
    assertScreenRoundTrips(OpenUrlScreen("https://slackhq.github.io/circuit/"))
    assertScreenRoundTrips(
      PetDetailScreen(
        petId = 42,
        photoUrlMemoryCacheKey = "puppy",
        animal = partialAnimal,
      )
    )
    assertScreenRoundTrips(
      PetPhotoCarouselScreen(
        id = 42,
        name = "Puppy",
        photoUrls = listOf("https://example.com/puppy.jpg"),
        photoUrlMemoryCacheKey = "puppy",
        photoAspectRatio = 1.5f,
      )
    )
    assertScreenRoundTrips(FiltersScreen(filters))
    assertPopResultRoundTrips(FiltersScreen.Result(filters))
    assertScreenRoundTrips(PetListScreen)
  }

  private inline fun <reified T : Screen> assertScreenRoundTrips(screen: T) {
    val saved = assertNotNull(starCircuitSaver.save(screen))
    val restored = assertNotNull(starCircuitSaver.restoreScreen<T>(saved))

    assertEquals(screen.snapshot(), restored.snapshot())
  }

  private inline fun <reified T : PopResult> assertPopResultRoundTrips(result: T) {
    val saved = assertNotNull(starCircuitSaver.save(result))
    val restored = assertNotNull(starCircuitSaver.restorePopResult<T>(saved))

    assertEquals(result.snapshot(), restored.snapshot())
  }

  private fun CircuitSaveable.snapshot(): Any =
    when (this) {
      is ListBenchmarksScreen -> listOf("ListBenchmarksScreen", useNestedContent)
      is ListBenchmarksItemScreen -> listOf("ListBenchmarksItemScreen", index)
      AboutScreen -> "AboutScreen"
      HomeScreen -> "HomeScreen"
      is ImageViewerScreen -> listOf("ImageViewerScreen", id, url, index, placeholderKey)
      is OpenUrlScreen -> listOf("OpenUrlScreen", url)
      is PetDetailScreen ->
        listOf(
          "PetDetailScreen",
          petId,
          photoUrlMemoryCacheKey,
          animal?.let {
            listOf(it.id, it.name, it.imageUrl, it.breed, it.gender, it.size)
          },
        )
      is PetPhotoCarouselScreen ->
        listOf(
          "PetPhotoCarouselScreen",
          id,
          name,
          photoUrls,
          photoUrlMemoryCacheKey,
          photoAspectRatio,
        )
      is FiltersScreen -> listOf("FiltersScreen", initialFilters.genders, initialFilters.sizes)
      is FiltersScreen.Result -> listOf("FiltersScreen.Result", filters.genders, filters.sizes)
      PetListScreen -> "PetListScreen"
      else -> error("Unexpected CircuitSaveable: $this")
    }
}
