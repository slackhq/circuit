Factories
=========

At its core, Circuit works on the Factory pattern. Every `Presenter` and `Ui` is contributed to a `CircuitConfig` instance by a corresponding factory that creates them for given `Screen`s. These are intended to be aggregated in the DI layer and added to a `CircuitConfig` instance during construction.

```kotlin
val circuitConfig = CircuitConfig.Builder()
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
  override fun create(screen: Screen, navigator: Navigator): Presenter<*>? {
    return when (screen) {
      is FavoritesScreen -> favoritesPresenterFactory.create(screen, navigator)
      else -> null
    }
  }
}
```

UI factories are similar, but generally should not aggregate other UIs unless there’s a DI-specific reason to do so (which there usually isn’t!).

```kotlin
class FavoritesScreenUiFactory : @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen): ScreenUi? {
    return when (screen) {
      is FavoritesScreen -> ScreenUi(favoritesUi())
      else null ->
    }
  }
}

private fun favoritesUi() = ui<State> { state -> Favorites(state) }
```

!!! info
    Note how these return a `ScreenUi` class that holds the Ui instance. We are using this indirection as a toe-hold for possible other future UI metadata, such as `Modifier` instances.

We canonically write these out as a separate function (`favoritesUi()`) that returns a `Ui`, which in turn calls through to the real (basic) Compose UI function (`Favorites()`). This ensures our basic compose functions are top-level and accessible by tests, and also discourages storing anything in class members rather than idiomatic composable state vars.
