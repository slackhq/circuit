package com.slack.circuit.sample.petdetail

import app.cash.molecule.RecompositionClock.Immediate
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.sample.petlist.PetListPresenterTest
import com.slack.circuit.sample.petlist.TestRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PetDetailPresenterTest {
  @Test
  fun `presesent - emit loading state then no animal state`() = runTest {
    val repository = TestRepository(emptyList())
    val screen = PetDetailScreen(123L, "key")
    val presenter = PetDetailPresenter(screen, repository)

    moleculeFlow(Immediate) { presenter.present(emptyFlow()) }
        .test {
            assertThat(PetDetailScreen.State.Loading).isEqualTo(awaitItem())
            assertThat(PetDetailScreen.State.NoAnimal).isEqualTo(awaitItem())
        }
  }

    @Test
    fun `presesent - emit loading state then success state`() = runTest {
        val animal = PetListPresenterTest.animal
        val repository = TestRepository(listOf(animal))
        val screen = PetDetailScreen(animal.id, animal.photos.first().small)
        val presenter = PetDetailPresenter(screen, repository)

        moleculeFlow(Immediate) { presenter.present(emptyFlow()) }
            .test {
                assertThat(PetDetailScreen.State.Loading).isEqualTo(awaitItem())

                val success = animal.run {
                    PetDetailScreen.State.Success(
                        url = url,
                        photoUrl = photos.first().large,
                        photoUrlMemoryCacheKey = photos.first().small,
                        name = name,
                        description = description
                    )
                }
                assertThat(success).isEqualTo(awaitItem())
            }
    }
}
