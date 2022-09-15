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
package com.slack.circuit.sample.home

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
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize

// Need better naming conventions.
@Parcelize
object CompositeScreen : Screen {
  data class CompositeState(val homeState: HomeScreen.State, val petListState: PetListScreen.State)

  sealed interface CompositeEvent {
    class CompositeHomeEvent(val event: HomeScreen.Event.NavClickEvent) : CompositeEvent
    class CompositePetListEvent(val event: PetListScreen.Event) : CompositeEvent
  }
}

@ContributesMultibinding(AppScope::class)
class CompositePresenterFactory
@Inject
constructor(private val compositePresenterFactory: CompositePresenter.Factory) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is CompositeScreen) return compositePresenterFactory.create(navigator)
    return null
  }
}

class CompositePresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val homePresenter: HomePresenter,
  petListPresenterFactory: PetListPresenter.Factory
) : Presenter<CompositeScreen.CompositeState, CompositeScreen.CompositeEvent> {
  private val petListPresenter = petListPresenterFactory.create(navigator)

  @SuppressLint("FlowOperatorInvokedInComposition")
  @Composable
  override fun present(
    events: Flow<CompositeScreen.CompositeEvent>
  ): CompositeScreen.CompositeState {
    val homeState =
      homePresenter.present(
        events.filterIsInstance<CompositeScreen.CompositeEvent.CompositeHomeEvent>().map {
          it.event
        }
      )
    val petListState =
      petListPresenter.present(
        events.filterIsInstance<CompositeScreen.CompositeEvent.CompositePetListEvent>().map {
          it.event
        }
      )

    return CompositeScreen.CompositeState(homeState, petListState)
  }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): CompositePresenter
  }
}

@ContributesMultibinding(AppScope::class)
class CompositeScreenFactory @Inject constructor() : ScreenViewFactory {
  override fun createView(screen: Screen): ScreenView? {
    if (screen is CompositeScreen) {
      return ScreenView(compositeUi())
    }
    return null
  }
}

private fun compositeUi() =
  ui<CompositeScreen.CompositeState, CompositeScreen.CompositeEvent> { state, events ->
    CompositeScreen(state, events)
  }

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun CompositeScreen(
  state: CompositeScreen.CompositeState,
  eventSink: (CompositeScreen.CompositeEvent) -> Unit
) {
  Scaffold(
    modifier = Modifier.systemBarsPadding().fillMaxWidth(),
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
      BottomNavigationBar(selectedIndex = state.homeState.index) { index ->
        eventSink(
          CompositeScreen.CompositeEvent.CompositeHomeEvent(HomeScreen.Event.NavClickEvent(index))
        )
      }
    },
    content = {
      PetList(state.petListState) { event ->
        eventSink(CompositeScreen.CompositeEvent.CompositePetListEvent(event))
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
        label = { Text(text = item.title) },
        selectedContentColor = Color.White,
        unselectedContentColor = Color.White.copy(0.4f),
        alwaysShowLabel = true,
        selected = selectedIndex == index,
        onClick = { onSelectedIndex(index) }
      )
    }
  }
}
