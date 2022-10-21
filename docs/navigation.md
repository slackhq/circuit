Navigation
==========

For navigable contents, we have a custom compose-based backstack implementation that the androidx folks shared with us. Navigation becomes two parts:

1. A `BackStack`, where we use a `SaveableBackStack` implementation that saves a stack of `Screen`s and the `ProvidedValues` for each record on that stack (allowing us to save and restore on configuration changes automatically).
2. A `Navigator`, which is a simple interface that we can point at a `BackStack` and offers simple `goTo(<screen>)`/`pop()` semantics. These are offered to presenters to perform navigation as needed to other screens.

A new navigable content surface is handled via the `NavigableCircuitContent` functions.

```kotlin
setContent {
  val backstack = rememberSaveableBackStack { push(HomeScreen) }
  val navigator = rememberCircuitNavigator(backstack, onBackPressedDispatcher::onBackPressed)
  NavigableCircuitContent(navigator, backstack)
}
```

!!! warning
    `SaveableBackStack` _must_ have a size of 1 or more after initialization. It's an error to have a backstack with zero items.

Presenters are then given access to these navigator instances via `Presenter.Factory` (described in TODO link Factories), which they can save if needed to perform navigation.

```kotlin
fun showAddFavorites() {
  navigator.goTo(
    AddFavorites(
      externalId = uuidGenerator.generate()
    )
  )
}
```

## Nested Navigation

Navigation carries special semantic value in `CircuitContent` as well, where it’s common for UIs to want to curry navigation events emitted by nested UIs. For this case, there’s a `CircuitContent` overload that accepts an optional onNavEvent callback that you must then forward to a Navigator instance.

```kotlin
@Composable fun ParentUi(state: ParentState) {
  CircuitContent(NestedScreen, onNavEvent = { navEvent -> state.eventSink(NestedNav(navEvent)) })
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
