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
package com.slack.circuit

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Presents a given [UiState] and handles its [events][UiEvent].
 *
 * If a given [Ui] never emits events, then you can use [Nothing] for the [UiEvent] type instead.
 *
 * @see present for more thorough documentation.
 */
interface Presenter<UiState : Any, UiEvent : Any> {
  /**
   * The primary [Composable] entry point to present a [UiState] and handle its [events]. In
   * production, a [Navigator] is used to automatically connect this with a corresponding [Ui] to
   * render the state returned by this function.
   *
   * When collecting [events], use [EventCollector] to collect them.
   *
   * ```kotlin
   * class FavoritesPresenter(...) : Presenter<State, Event> {
   *   @Composable override fun present(events: Flow<Event>): State {
   *     var state by remember { mutableStateOf(initialState) }
   *     // ...
   *     EventCollector(events) { event ->
   *       // Handle UI events here
   *     }
   *
   *     return state
   *   }
   * }
   * ```
   *
   * ## Dependency Injection
   *
   * Presenters should use dependency injection, usually assisted injection to accept [Navigator] or
   * [Screen] instances as inputs. Their corresponding assisted factories should then be used by
   * hand-written [presenter factories][Presenter.Factory].
   *
   * ```kotlin
   * class FavoritesPresenter @AssistedInject constructor(
   *   @Assisted private val screen: FavoritesScreen,
   *   @Assisted private val navigator: Navigator,
   *   private val favoritesRepository: FavoritesRepository
   * ) : Presenter<State, Event> {
   *   @Composable override fun present(events: Flow<Event>): State {
   *     // ...
   *   }
   *
   *   @AssistedFactory
   *   fun interface Factory {
   *     fun create(screen: FavoritesScreen, navigator: Navigator): FavoritesPresenter
   *   }
   * }
   * ```
   *
   * ## Testing
   *
   * When testing, simply drive UI events with a [MutableSharedFlow] use Molecule+Turbine to drive
   * this function.
   *
   * ```
   * @Test
   * fun `emit initial state and refresh`() = runTest {
   *   val favorites = listOf("Moose", "Reeses", "Lola")
   *   val repository = FakeFavoritesRepository(favorites)
   *   val presenter = FavoritesPresenter(repository)
   *   val events = MutableSharedFlow<Event>()
   *
   *   moleculeFlow(Immediate) { presenter.present(events) }
   *     .test {
   *       assertThat(awaitItem()).isEqualTo(State.Loading)
   *       assertThat(awaitItem()).isEqualTo(State.Success(favorites))
   *       events.emit(Event.Refresh)
   *       assertThat(awaitItem()).isEqualTo(State.Success(favorites))
   *     }
   * }
   * ```
   */
  @Composable fun present(events: Flow<UiEvent>): UiState

  /**
   * A factory that produces [presenters][Presenter] for a given [Screen]. [Circuit] instances use
   * the created presenter and connects it to a given [Ui] for the same [Screen].
   *
   * Factories should be simple aggregate multiple presenters for a canonical "whole screen". That
   * is to say, they should be hand-written and aggregate all the presenters responsible for the UI
   * visible within the surface this presents on.
   *
   * ## Example
   *
   * Consider this example of a Profile UI.
   *
   * ```
   *                           ┌────────────────────┐
   *                      ┌─── │                    │
   *                      │    ├────────────────────┤◄──┐
   *                      │    │ X                  │   │
   *                      │    │                    │ ProfileHeaderPresenter
   *                      │    │ Fred Rogers        │   │
   *                      │    ├────────────────────┤◄──┘
   *                      │    │ ┌───────┐  ┌────┐  │
   * ProfilePresenterFactory   │ │Message│  │Call│◄─┼─── ProfileActionsPresenter
   *                      │    │ └───────┘  └────┘  │
   *                      │    │                    │
   *                      │    │  - - - - - - - - ◄─┼────┐
   *                      │    │  - - - - - - - -   │    │
   *                      │    │  - - - - - - - -   │  ProfileDetailsPresenter
   *                      │    │  - - - - - - - - ◄─┼────┘
   *                      └─── │                    │
   *                           └────────────────────┘
   * ```
   *
   * This would be represented by the following factory implementation:
   *
   * ```kotlin
   * class ProfilePresenter.Factory @Inject constructor(
   *   val headerPresenter: ProfilerHeaderPresenter.Factory,
   *   val actionsPresenter: ProfilerActionsPresenter.Factory,
   *   val detailsPresenter: ProfilerDetailsPresenter.Factory,
   *   val callScreenRouter: CallScreenRouter.Factory
   * ) : Presenter.Factory {
   *   override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
   *     return when (screen) {
   *       is ProfileHeader -> headerPresenter.create(screen)
   *       is ProfileActions -> actionsPresenter.create(screen, callScreenRouter.create(navigator))
   *       is ProfileDetails -> detailsPresenter.create(screen)
   *       else -> null
   *     }
   *   }
   * }
   * ```
   */
  // Diagram generated from asciiflow: https://shorturl.at/fgjtA
  fun interface Factory {
    /**
     * Creates a [Presenter] for the given [screen] if it can handle it, or returns null if it
     * cannot handle the given [screen].
     */
    fun create(screen: Screen, navigator: Navigator): Presenter<*, *>?
  }
}
