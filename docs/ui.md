UI
==

The core Ui interface is simply this:

```kotlin
interface Ui<UiState : CircuitUiState> {
  @Composable fun Content(state: UiState, modifier: Modifier)
}
```

Like presenters, simple UIs can also skip the class all together for use in other UIs. Core unit of granularity is just the @Composable function. In fact, when implementing these in practice they rarely use dependency injection at all and can normally just be written as top-level composable functions annotated with `@CircuitInject`.

```kotlin
@CircuitInject(FavoritesScreen::class, AppScope::class) // Relevant DI wiring is generated
@Composable
private fun Favorites(state: FavoritesState, modifier: Modifier = Modifier) {
  // ...
}
```

Writing UIs like this has a number of benefits.

* Functions-only nudges developers toward writing idiomatic compose code and not keeping un-scoped/un-observable state elsewhere (such as class properties).
* These functions are extremely easy to stand up in tests.
* These functions are extremely easy to stand up in Compose _preview_ functions.


Let’s look a little more closely at the last bullet point about preview functions. With the above example, we can easily stand up previews for all of our different states!

```kotlin
@Preview
@Composable
private fun PreviewFavorites() = Favorites(FavoritesState(listOf("Reeses", "Lola")))

@Preview
@Composable
private fun PreviewEmptyFavorites() = Favorites(FavoritesState(listOf()))
```

## Static UI

In some cases, a UI may not need a presenter to compute or manage its state. Examples of this include UIs that are stateless or can derive their state from a single static input or an input [Screen]'s properties. In these cases, make your _screen_ implement the `StaticScreen` interface. When a `StaticScreen` is used, Circuit will internally allow the UI to run on its own and won't connect it to a presenter if no presenter is provided.
