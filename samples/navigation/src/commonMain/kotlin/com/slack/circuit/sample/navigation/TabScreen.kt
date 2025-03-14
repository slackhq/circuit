package com.slack.circuit.sample.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sample.navigation.parcel.CommonParcelize
import kotlin.reflect.KClass
import kotlinx.collections.immutable.persistentListOf

@CommonParcelize
sealed interface TabScreen : Screen {
  val label: String

  @CommonParcelize data class Root(override val label: String = "Root") : TabScreen

  @CommonParcelize data class Screen1(override val label: String = "Screen 1") : TabScreen

  @CommonParcelize data class Screen2(override val label: String = "Screen 2") : TabScreen

  @CommonParcelize data class Screen3(override val label: String = "Screen 3") : TabScreen

  fun next(): TabScreen {
    return when (this) {
      is Root -> Screen1()
      is Screen1 -> Screen2()
      is Screen2 -> Screen3()
      is Screen3 -> Root()
    }
  }

  companion object {
    val all = persistentListOf(Root(), Screen1(), Screen2(), Screen3())
  }
}

object TabScreenCircuit {

  data class State(val label: String, val eventSink: (Event) -> Unit) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object Next : Event
  }
}

class TabPresenter(private val screen: TabScreen, private val navigator: Navigator) :
  Presenter<TabScreenCircuit.State> {
  @Composable
  override fun present(): TabScreenCircuit.State {
    return TabScreenCircuit.State(label = screen.label) { event ->
      when (event) {
        TabScreenCircuit.Event.Next -> navigator.goTo(screen.next())
      }
    }
  }

  class Factory(private val tabClass: KClass<out TabScreen>) : Presenter.Factory {
    override fun create(
      screen: Screen,
      navigator: Navigator,
      context: CircuitContext,
    ): Presenter<*>? {
      return if (tabClass.isInstance(screen)) {
        TabPresenter(screen as TabScreen, navigator)
      } else {
        null
      }
    }
  }
}

@Composable
fun TabUI(state: TabScreenCircuit.State, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier.fillMaxSize().testTag(ContentTags.TAG_CONTENT).clickable {
        state.eventSink(TabScreenCircuit.Event.Next)
      }
  ) {
    Text(
      text = state.label,
      modifier = Modifier.testTag(ContentTags.TAG_LABEL).align(Alignment.Center),
    )
  }
}

class TabUiFactory(private val tabClass: KClass<out TabScreen>) : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return if (tabClass.isInstance(screen)) {
      ui<TabScreenCircuit.State> { state, modifier -> TabUI(state, modifier) }
    } else {
      null
    }
  }
}
