UI
==

The core Ui interface is simply this:

```kotlin
interface Ui<UiState : CircuitUiState> {
  @Composable fun Content(state: UiState, modifier: Modifier)
}
```

Like presenters, simple UIs can also skip the class all together for use in other UIs. Core unit of granularity is just the @Composable function. In fact, when implementing these in practice they rarely use dependency injection at all and can normally just be written as top-level composable functions annotated with` @CircuitInject`.

```kotlin
@CircuitInject<FavoritesScreen> // Relevant DI wiring is generated
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

TODO image sample of IDE preview
