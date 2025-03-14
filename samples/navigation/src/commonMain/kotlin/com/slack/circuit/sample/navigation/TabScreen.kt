package com.slack.circuit.sample.navigation

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@CommonParcelize
sealed interface TabScreen : Screen {
  val label: String

  data object Root : TabScreen {
    override val label: String = "Root"
  }

  data object Screen1 : TabScreen {
    override val label: String = "Screen 1"
  }

  data object Screen2 : TabScreen {
    override val label: String = "Screen 2"
  }

  data object Screen3 : TabScreen {
    override val label: String = "Screen 3"
  }

  fun next(): TabScreen {
    return when (this) {
      Root -> Screen1
      Screen1 -> Screen2
      Screen2 -> Screen3
      Screen3 -> Root
    }
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
  Text(
    text = state.label,
    modifier =
      modifier.testTag(ContentTags.TAG_LABEL).clickable {
        state.eventSink(TabScreenCircuit.Event.Next)
      },
  )
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
