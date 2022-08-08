package com.slack.circuit

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

/**
 * A simple function that can [render] [UiState]. Usually sugar over [Ui.render] but defined
 * separately to keep the bones of the framework flexible.
 */
fun interface StateRenderer<UiState, UiEvent : Any> where UiState : Any, UiState : Parcelable {
  /** Renders a given [state] and emits [UI events][UiEvent] back up to [uiEvents]. */
  @Composable fun render(state: UiState, uiEvents: (UiEvent) -> Unit)

  /** Shorthand for [render]. */
  // do not override!
  @Composable
  operator fun invoke(state: UiState, uiEvents: (UiEvent) -> Unit) = render(state, uiEvents)
}

/**
 * Presents a given [UiState] and handles its [events][UiEvent].
 *
 * TODO doc factory patterns TODO doc accepting Screens and Navigator params
 */
interface Presenter<UiState, UiEvent : Any> where UiState : Any, UiState : Parcelable {
  /**
   * The primary entry point to present a [Composable] [render] to connect this [Presenter] with a
   * given [Ui], usually automatically handled by a [Navigator]. The structure of this function is
   * based around two parameters:
   * 1. `state` - the current `UiState`.
   * 2. `uiEvents` - a callback to listen to UiEvent emissions from the corresponding [Ui].
   *
   * Then when implementing, the [render] function is the jumping off point to the [Ui]. Usually,
   * state is modeled as a [mutableStateOf] and then passed in to the [render] function. Then any
   * time the state updates, the [render] function is recomposed accordingly.
   *
   * ```kotlin
   * class FavoritesPresenter : Presenter<State, Event> {
   *   @Composable override fun present(render: Renderer<State, Event>) {
   *     var state by rememberSaveable { mutableStateOf(initialState) }
   *     // ...
   *     render(state) { event ->
   *       // Handle UI events here
   *     }
   *   }
   * }
   * ```
   */
  @Composable fun present(render: StateRenderer<UiState, UiEvent>)
}

fun interface PresenterFactory {
  fun create(screen: Screen, navigator: Navigator): Presenter<*, *>?
}

// Example
// class FavoritesPresenterFactory @Inject constructor(
//  private val addFavoritesPresenter: AddFavoritesPresenter.Factory,
// ) : PresenterFactory {
//
//  override fun create(
//    screen: Screen,
//    navigator: Navigator
//  ): Presenter<*, *>? {
//    return when (screen) {
//      is AddFavorites -> addFavoritesPresenter.create(
//        navigator = navigator,
//        args = screen,
//      ).asPresenter()
//      else -> null
//    }
//  }
// }
