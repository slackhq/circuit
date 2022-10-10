Presenter
=========

The core Presenter interface is this:

```kotlin
interface Presenter<UiState : CircuitUiState> {
  @Composable fun present(): UiState
}
```

Presenters are solely intended to be business logic for your UI and a translation layer in front of your data layers. They are generally Dagger-injected types as the data layers they interpret are usually coming from the DI graph. In simple cases, they can be typed as a simple `@Composable` presenter function and Circuit code gen (TODO link) can generate the corresponding interface and (TODO link) factory for you.

A very simple presenter can look like this:

```kotlin
class FavoritesPresenter(...) : Presenter<State> {
  @Composable override fun present(): State {
    var favorites by remember { mutableStateOf(<initial>) }
    
    return State(favorites) { event -> ... }
  }
}
```

In this example, the `present()` function simply computes a `state` and returns it. If it has UI events to handle, an `eventSink: (Event) -> Unit` property should be exposed in the `State` type it returns.

With DI, the above example becomes something more like this:

```kotlin
class FavoritesPresenter @AssistedInject constructor(
  @Assisted private val screen: FavoritesScreen,
  @Assisted private val navigator: Navigator,
  private val favoritesRepository: FavoritesRepository
) : Presenter<State> {
  @Composable override fun present(): State {
    // ...
  }
  @AssistedFactory
  fun interface Factory {
    fun create(screen: FavoritesScreen, navigator: Navigator): FavoritesPresenter
  }
}
```

Assisted injection allows passing on the `screen` and `navigator` from the relevant `Presenter.Factory` to this presenter for further reference.

When dealing with nested presenters, a presenter could bypass implementing a class entirely by simply being written as a function that other presenters can use. 

```kotlin
// From cashapp/molecule's README examples
@Composable
fun ProfilePresenter(
  userFlow: Flow<User>,
  balanceFlow: Flow<Long>,
): ProfileModel {
  val user by userFlow.collectAsState(null)
  val balance by balanceFlow.collectAsState(0L)

  return if (user == null) {
    Loading
  } else {
    Data(user.name, balance)
  }
}
```

Presenters can present other presenters by injecting their assisted factories/providers, but note that this makes them a composite presenter that is now assuming responsibility for managing state of multiple nested presenters. [We have an example of this in the Circuit repo](https://github.com/slackhq/circuit/blob/main/samples/star/src/main/kotlin/com/slack/circuit/star/home/HomePresenter.kt).

## Retention

There are three types of composable retention functions used in Circuit.

1. `remember` – from Compose, remembers a value across recompositions. Can be any type.
2. `rememberRetained` – custom, remembers a value across recompositions and configuration changes. Can be any type, but should not retain leak-able things like `Navigator` instances or `Context` instances. Backed by a hidden `ViewModel` on Android.
3. `rememberSaveable` – from Compose, remembers a value across recompositions, configuration changes, and process death. Must be `Parcelable` or implement a custom `Saver`, should not retain leakable things like `Navigator` instances or `Context` instances. Backed by the framework saved instance state system.

Developers should use the right tool accordingly depending on their use case. Consider these three examples.

The first one will preserve the `count` value across recompositions, but not configuration changes or process death.

```kotlin
@Composable
fun CounterPresenter(): CounterState {
  var count by remember { mutableStateOf(0) }

  return CounterState(count) { event ->
    when (event) {
      is CounterEvent.Increment -> count++
      is CounterEvent.Decrement -> count--
    }
  }
}
```

The second one will preserve the state across recompositions and configuration changes, but not process death.

```kotlin
@Composable
fun CounterPresenter(): CounterState {
  var count by rememberRetained { mutableStateOf(0) }

  return CounterState(count) { event ->
    when (event) {
      is CounterEvent.Increment -> count++
      is CounterEvent.Decrement -> count--
    }
  }
}
```

The third case will preserve the `count` state across recompositions, configuration changes, and process death. However, it only works with primitives or `Parcelable` state types.

```kotlin
@Composable
fun CounterPresenter(): CounterState {
  var count by rememberSaveable { mutableStateOf(0) }

  return CounterState(count) { event ->
    when (event) {
      is CounterEvent.Increment -> count++
      is CounterEvent.Decrement -> count--
    }
  }
}
```
