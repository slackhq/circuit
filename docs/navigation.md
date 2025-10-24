Navigation
==========

For navigable contents, we have a custom compose-based backstack implementation that the androidx folks shared with us. Navigation becomes two parts:

1. A `BackStack`, where we use a `SaveableBackStack` implementation that saves a stack of `Screen`s and the `ProvidedValues` for each record on that stack (allowing us to save and restore on configuration changes automatically).
2. A `Navigator`, which is a simple interface that we can point at a `BackStack` and offers simple `goTo(<screen>)`/`pop()` semantics. These are offered to presenters to perform navigation as needed to other screens.

A new navigable content surface is handled via the `NavigableCircuitContent` functions.

```kotlin
setContent {
  val backStack = rememberSaveableBackStack(root = HomeScreen)
  val navigator = rememberCircuitNavigator(backStack)
  NavigableCircuitContent(navigator, backStack)
}
```

!!! warning
    `SaveableBackStack` _must_ have a size of 1 or more after initialization. It's an error to have a backstack with zero items.

Presenters are then given access to these navigator instances via `Presenter.Factory` (described in [Factories](https://slackhq.github.io/circuit/factories/)), which they can save if needed to perform navigation.

```kotlin
fun showAddFavorites() {
  navigator.goTo(
    AddFavorites(
      externalId = uuidGenerator.generate()
    )
  )
}
```

If you want to have custom behavior for when back is pressed on the root screen (i.e. `backstack.size == 1`), you should implement your own `BackHandler` and use it _before_ creating the backstack.

```kotlin
setContent {
  val backStack = rememberSaveableBackStack(root = HomeScreen)
  BackHandler(onBack = { /* do something on root */ })
  // The Navigator's internal BackHandler will take precedence until it is at the root screen.
  val navigator = rememberCircuitNavigator(backStack)
  NavigableCircuitContent(navigator, backStack)
}
```

#### Deep Linking

Circuit allows initializing a stack of screens in the navigator, which is useful for supporting deep linking into the application.

For more details on handling deep links and manipulating the back stack, refer to the [deep linking guide](deep-linking-android.md) for Android.

## Results

In some cases, it makes sense for a screen to return a result to the previous screen. This is done by using the _answering Navigator_ pattern in Circuit.

The primary entry point for requesting a result is the `rememberAnsweringNavigator` API, which takes a `Navigator` or `BackStack` and `PopResult` type and returns a navigator that can go to a screen and await a result.

Result types must implement `PopResult` and are used to carry data back to the previous screen.

The returned navigator should be used to navigate to the screen that will return the result. The target screen can then `pop` the result back to the previous screen and Circuit will automatically deliver this result to the previous screen's receiver.

```kotlin
var photoUri by remember { mutableStateOf<String?>(null) }
val takePhotoNavigator = rememberAnsweringNavigator<TakePhotoScreen.Result>(navigator) { result ->
  photoUri = result.uri
}

// Elsewhere
takePhotoNavigator.goTo(TakePhotoScreen)

// In TakePhotoScreen.kt
data object TakePhotoScreen : Screen {
  @Parcelize
  data class Result(val uri: String) : PopResult
}

class TakePhotoPresenter {
  @Composable fun present(): State {
    // ...
    navigator.pop(result = TakePhotoScreen.Result(photoUri))
  }
}
```

Circuit automatically manages saving/restoring result states and ensuring that results are only delivered to the original receiver that requested it. If the target screen does not pop back a result, the previous screen's receiver will just never receive one.

!!! note "When to use an `Overlay` vs navigating to a `Screen` with result?"
    See this doc in [Overlays](https://slackhq.github.io/circuit/overlays/#overlay-vs-popresult)!

## Nested Navigation

Navigation carries special semantic value in `CircuitContent` as well, where it’s common for UIs to want to curry navigation events emitted by nested UIs. For this case, there’s a `CircuitContent` overload that accepts an optional onNavEvent callback that you must then forward to a Navigator instance.

```kotlin
@Composable fun ParentUi(state: ParentState, modifier: Modifier = Modifier) {
  CircuitContent(NestedScreen, modifier = modifier, onNavEvent = { navEvent -> state.eventSink(NestedNav(navEvent)) })
}

@Composable fun ParentPresenter(navigator: Navigator): ParentState {
  return ParentState(...) { event ->
    when (event) {
      is NestedNav -> navigator.onNavEvent(event.navEvent)
    }
  }
}

@Composable 
fun NestedPresenter(navigator: Navigator): NestedState {
  // These are forwarded up!
  navigator.goTo(AnotherScreen)
  
  // ...
}
```
