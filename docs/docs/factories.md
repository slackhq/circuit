Factories
=========

At its core, Circuit works on the Factory pattern. Every `Presenter` and `Ui` is contributed to a `Circuit` instance by a corresponding factory that creates them for given `Screen`s. These are intended to be aggregated in the DI layer and added to a `Circuit` instance during construction.

```kotlin
val circuit = Circuit.Builder()
  .addUiFactory(FavoritesUiFactory())
  .addPresenterFactory(FavoritesPresenterFactory())
  .build()
```

!!! tip "Look familiar?"
    If you’ve used Moshi or Retrofit, these should feel fairly familiar!

Presenter factories can be generated or hand-written, depending on if they aggregate an entire screen or are simple one-offs. Presenters are also given access to the current Navigator in this level.

```kotlin
class FavoritesScreenPresenterFactory @Inject constructor(
  private val favoritesPresenterFactory: FavoritesPresenter.Factory,
) : Presenter.Factory {
  override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
    return when (screen) {
      is FavoritesScreen -> favoritesPresenterFactory.create(screen, navigator, context)
      else -> null
    }
  }
}
```

UI factories are similar, but generally should not aggregate other UIs unless there’s a DI-specific reason to do so (which there usually isn’t!).

```kotlin
class FavoritesScreenUiFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return when (screen) {
      is FavoritesScreen -> favoritesUi()
      else -> null
    }
  }
}

private fun favoritesUi() = ui<State> { state, modifier -> Favorites(state, modifier) }
```

!!! info
    Note how these include a `Modifier`. You should pass on these modifiers to your UI. [Always provide a modifier!](https://chris.banes.me/posts/always-provide-a-modifier/)

We canonically write these out as a separate function (`favoritesUi()`) that returns a `Ui`, which in turn calls through to the real (basic) Compose UI function (`Favorites()`). This ensures our basic compose functions are top-level and accessible by tests, and also discourages storing anything in class members rather than idiomatic composable state vars. If you use code gen, it handles the intermediate function for you.
