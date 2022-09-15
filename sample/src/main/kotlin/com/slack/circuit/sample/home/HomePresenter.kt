package com.slack.circuit.sample.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.sample.di.AppScope
import com.squareup.anvil.annotations.ContributesMultibinding
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object HomeScreen : Screen {
  @Immutable
  data class State(
    val index: Int = 0,
    val bottomNavItems: List<Screen>
  )

  sealed interface Event {
    @Immutable
    data class NavClickEvent(val index: Int) : Event
  }
}

@ContributesMultibinding(AppScope::class)
class HomeScreenPresenterFactory
@Inject
constructor(private val homePresenterFactory: HomePresenter) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is HomeScreen) return homePresenterFactory
    return null
  }
}

class HomePresenter @Inject constructor() : Presenter<HomeScreen.State, HomeScreen.Event> {
  private val homeScreenNavItems = listOf(BottomNavItem.Dogs.screen, BottomNavItem.Cats.screen)

  @Composable
  override fun present(events: Flow<HomeScreen.Event>): HomeScreen.State {
    var state by remember {
      mutableStateOf(HomeScreen.State(bottomNavItems = homeScreenNavItems))
    }

    // LaunchedEffect makes it take two clicks, figure it out when i come back.
    LaunchedEffect(events) {
      events.collect { event ->
        when(event) {
          is HomeScreen.Event.NavClickEvent -> state = state.copy(index = event.index)
        }
      }
    }
    return state
  }
}
