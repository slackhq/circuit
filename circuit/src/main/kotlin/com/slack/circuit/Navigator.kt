package com.slack.circuit

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.NavigatorDefaults
import com.slack.circuit.backstack.NavigatorRouteDecoration
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.providedValuesForBackStack
import com.slack.circuit.backstack.push
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.backstack.screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

fun interface OnPopHandler {
  fun onPop()
}

/** TODO doc this */
interface Navigator {
  fun goTo(screen: Screen)

  fun pop()

  fun interface Factory<T : Navigator> {
    fun create(renderer: ContentContainer, onRootPop: OnPopHandler?): T
  }
}

/**
 * A simple rendering abstraction over a `@Composable () -> Unit`.
 *
 * This allows for any host container that can render composable functions to be used with a given
 * [Navigator.Factory.create] call, such as [ComponentActivity] and [ComposeView].
 */
fun interface ContentContainer {
  fun render(content: @Composable () -> Unit)
}

fun ComposeView.asContentContainer() = ContentContainer(::setContent)

fun ComponentActivity.asContentContainer() = ContentContainer { content ->
  setContent(content = content)
}

class NavigatorImpl
@AssistedInject
constructor(
  @Assisted val container: ContentContainer,
  @Assisted val onRootPop: OnPopHandler?,
  // TODO do we ever handle precedence/order here?
  val presenterFactories: @JvmSuppressWildcards Set<PresenterFactory>,
  val uiFactories: @JvmSuppressWildcards Set<ScreenViewFactory>,
) : Navigator {

  @AssistedFactory fun interface Factory : Navigator.Factory<NavigatorImpl>

  override fun goTo(screen: Screen) {
    // Headless? - MutableStateFlow and launch
    // Testing - molecule to convert it to a Flow, then plumb into Turbine and treat it as a
    // coroutines test. Don't ever use Dispatchers default/io in tests
    container.render {
      val backstack = rememberSaveableBackStack { push(screen) }
      val goTo: (Screen) -> Unit = { screen -> backstack.push(screen) }
      val navigator =
        object : Navigator {
          override fun goTo(screen: Screen) {
            goTo(screen)
          }

          override fun pop() {
            if (!backstack.isAtRoot) {
              backstack.pop()
            } else {
              this@NavigatorImpl.pop()
            }
          }
        }
      FactoryNavigator(backstack, presenterFactories, uiFactories, navigator, container)
    }
  }

  override fun pop() {
    onRootPop?.onPop()
  }
}

@JvmInline
private value class UiStateRenderer<UiState, UiEvent : Any>(val ui: Ui<UiState, UiEvent>) :
  StateRenderer<UiState, UiEvent> where UiState : Any, UiState : Parcelable {
  @Composable
  override fun render(state: UiState, uiEvents: (UiEvent) -> Unit) {
    ui.render(state, uiEvents)
  }
}

@Composable
fun <R : BackStack.Record> FactoryNavigator(
  backStack: BackStack<R>,
  presenterFactories: Set<PresenterFactory>,
  uiFactories: Set<ScreenViewFactory>,
  navigator: Navigator,
  container: ContentContainer,
  modifier: Modifier = Modifier,
  enableBackHandler: Boolean = true,
  providedValues: Map<R, ProvidedValues> = providedValuesForBackStack(backStack),
  decoration: NavigatorRouteDecoration = NavigatorDefaults.DefaultDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  BackHandler(enabled = enableBackHandler && !backStack.isAtRoot) { backStack.pop() }

  BasicFactoryNavigator(
    backStack = backStack,
    presenterFactories = presenterFactories,
    uiFactories = uiFactories,
    navigator = navigator,
    container = container,
    providedValues = providedValues,
    modifier = modifier,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
  )
}

@Composable
fun <R : BackStack.Record> BasicFactoryNavigator(
  backStack: BackStack<R>,
  presenterFactories: Set<PresenterFactory>,
  uiFactories: Set<ScreenViewFactory>,
  navigator: Navigator,
  container: ContentContainer,
  providedValues: Map<R, ProvidedValues>,
  modifier: Modifier = Modifier,
  decoration: NavigatorRouteDecoration = NavigatorDefaults.EmptyDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  val currentPresenters by rememberUpdatedState(presenterFactories)
  val currentUis by rememberUpdatedState(uiFactories)

  val activeContentProviders = buildList {
    for (record in backStack) {
      val provider =
        key(record.key) {
          val routeName = record.route
          // TODO fix this
          val screen = (record as SaveableBackStack.Record).screen

          @Suppress("UNCHECKED_CAST")
          val ui =
            currentUis.firstNotNullOfOrNull { it.createView(screen, container) }?.ui
              as Ui<Parcelable, Any>?

          @Suppress("UNCHECKED_CAST")
          val presenter =
            currentPresenters.firstNotNullOfOrNull {
              // TODO fix exposition of navigator/backstack
              it.create(screen, navigator)
            } as Presenter<Parcelable, Any>?

          val currentRender: (@Composable (R) -> Unit) =
            if (presenter != null && ui != null) {
              { presenter.present(UiStateRenderer(ui)) }
            } else {
              { unavailableRoute(routeName) }
            }

          val currentRouteContent by rememberUpdatedState(currentRender)
          val currentRecord by rememberUpdatedState(record)
          remember { movableContentOf { currentRouteContent(currentRecord) } }
        }
      add(record to provider)
    }
  }

  if (backStack.size > 0) {
    @Suppress("SpreadOperator")
    decoration.DecoratedContent(activeContentProviders.first(), backStack.size, modifier) {
      (record, provider) ->
      val values = providedValues[record]?.provideValues()
      val providedLocals = remember(values) { values?.toTypedArray() ?: emptyArray() }
      CompositionLocalProvider(*providedLocals) { provider.invoke() }
    }
  }
}
