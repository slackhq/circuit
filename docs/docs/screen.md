Screen
======

Screens are keys for Presenter and UI pairings.

The core `Screen` interface is this:

```kotlin
interface Screen : CircuitSaveable
```

`Screen` does not require a particular persistence format. A `Screen` can be a simple marker `data object` or a `data class` with information to pass on.

```kotlin
@Serializable
data object HomeScreen : Screen

@Serializable
data class AddFavoritesScreen(val externalId: UUID) : Screen
```

Circuit's documentation uses kotlinx-serialization for screens and results by default. The annotation supplies a serializer. Persistence setup is covered below.

These are used by `Navigator`s (when called from presenters) or `CircuitContent` (when called from UIs) to start a new sub-circuit or nested circuit.

```kotlin
// In a presenter class
fun showAddFavorites() {
  navigator.goTo(
    AddFavoritesScreen(
      externalId = uuidGenerator.generate()
    )
  )
}
```

The information passed into a screen can also be used to interact with the data layer. In the example here, we are getting the `externalId` from the screen in order to get information back from our repository.

```kotlin
// In a presenter class
class AddFavoritesPresenter
@AssistedInject
constructor(
  @Assisted private val screen: AddFavoritesScreen,
  private val favoritesRepository: FavoritesRepository,
) : Presenter<AddFavoritesScreen.State> {
  @Composable
  override fun present() : AddFavoritesScreen.State {
      val favorite = favoritesRepository.getFavorite(screen.externalId)
      // ...
  }
}
```

Screens are also used to look up those corresponding components in `Circuit`.

```kotlin
val presenter: Presenter<*>? = circuit.presenter(addFavoritesScreen, navigator)
val ui: Ui<*>? = circuit.ui(addFavoritesScreen)
```

!!! tip "Nomenclature"
    Semantically, in this example we would call all of these components together the "AddFavorites Screen".

## Saving and restoring

Saveable navigation stacks use a `CircuitSaver` to persist their `Screen` and `PopResult` values. See [Saving navigation state](navigation-persistence.md) to choose a persistence strategy, provide the saver, and migrate from versions where these types were Parcelable on Android.
