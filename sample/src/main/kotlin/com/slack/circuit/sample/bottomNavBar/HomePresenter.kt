package com.slack.circuit.sample.bottomNavBar

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenView
import com.slack.circuit.ScreenViewFactory
import com.slack.circuit.sample.R
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.petlist.PetList
import com.slack.circuit.sample.petlist.PetListPresenter
import com.slack.circuit.sample.petlist.PetListScreen
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object HomeScreen : Screen {
  @Immutable
  data class State(
    val index: Int,
    val bottomNavItems: List<Screen>,
    val petListState: PetListScreen.State? = null
  )

  sealed interface Event {
    @Immutable
    data class NavClickEvent(val index: Int) : Event
    data class PetListEvent(val event: PetListScreen.Event) : Event
  }
}

@ContributesMultibinding(AppScope::class)
class HomeScreenPresenterFactory
@Inject
constructor(private val homePresenterFactory: HomePresenter.Factory) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is HomeScreen) return homePresenterFactory.create(navigator)
    return null
  }
}

class HomePresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  petListPresenterFactory: PetListPresenter.Factory
) : Presenter<HomeScreen.State, HomeScreen.Event> {
  private val petListPresenter = petListPresenterFactory.create(navigator)
  private val homeScreenNavItems = listOf(PetListScreen, PetListScreen)

  @SuppressLint("FlowOperatorInvokedInComposition")
  @Composable
  override fun present(events: Flow<HomeScreen.Event>): HomeScreen.State {
    var state by remember {
      mutableStateOf(HomeScreen.State(0, homeScreenNavItems))
    }

    // Nav click events.
    val filteredNavClickEventEvents = rememberFilteredHomeEvents(events)
    filteredNavClickEventEvents.map { event -> state = state.copy(index = event.index) }

    // Pet events.
    val filteredPetEvents = rememberFilteredPetEvents(events)
    val listState = petListPresenter.present(filteredPetEvents)
    state = state.copy(petListState = listState)

    return state
  }

  @Composable
  private fun rememberFilteredHomeEvents(events: Flow<HomeScreen.Event>): Flow<HomeScreen.Event.NavClickEvent> =
    remember(events) {
      events.filterIsInstance<HomeScreen.Event.NavClickEvent>().map { it }
    }

  @Composable
  private fun rememberFilteredPetEvents(events: Flow<HomeScreen.Event>): Flow<PetListScreen.Event> =
    remember(events) {
      events.filterIsInstance<HomeScreen.Event.PetListEvent>().map { it.event }
    }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): HomePresenter
  }
}

@ContributesMultibinding(AppScope::class)
class HomeScreenFactory @Inject constructor() : ScreenViewFactory {
  override fun createView(screen: Screen): ScreenView? {
    if (screen is HomeScreen) {
      return ScreenView(homeScreenUi())
    }
    return null
  }
}

private fun homeScreenUi() =
  ui<HomeScreen.State, HomeScreen.Event> { state, events -> HomeScreen(state, events) }

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun HomeScreen(state: HomeScreen.State, events: (HomeScreen.Event) -> Unit) {
  Scaffold(
    modifier = Modifier
      .systemBarsPadding()
      .fillMaxWidth(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text("Adoptables", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        colors =
        TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        )
      )
    },
    bottomBar = {
      BottomNavigationBar(selectedIndex = state.index) { index ->
        events(
          HomeScreen.Event.NavClickEvent(
            index
          )
        )
      }
    },
    content = {
      state.petListState?.let {
        PetList(it) { event ->
          events(HomeScreen.Event.PetListEvent(event))
        }
      }
    }
  )
}

@Composable
fun BottomNavigationBar(selectedIndex: Int, onSelectedIndex: (Int) -> Unit) {
  // These are the buttons on the NavBar, they dictate where we navigate too.
  val items = listOf(BottomNavItem.Dogs, BottomNavItem.Cats)
  BottomNavigation(
    backgroundColor = MaterialTheme.colorScheme.primary,
    contentColor = Color.White
  ) {
    items.forEachIndexed { index, item ->
      BottomNavigationItem(
        icon = {
          Icon(
            painterResource(id = R.drawable.drago_dog),
            contentDescription = item.title,
            modifier = Modifier.scale(0.5f)
          )
        },
        label = { androidx.compose.material.Text(text = item.title) },
        selectedContentColor = Color.White,
        unselectedContentColor = Color.White.copy(0.4f),
        alwaysShowLabel = true,
        selected = selectedIndex == index,
        onClick = {
          onSelectedIndex(index)
        }
      )
    }
  }
}