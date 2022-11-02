Screen
======

Screens are keys for Presenter and UI pairings. Semantically, the pairing of a Presenter and UI
for a given `Screen` key is called a circuit.

The core `Screen` interface is this:

```kotlin
interface Screen : Parcelable
```

These types are `Parcelable` for saveability in our backstack and easy deeplinking. A `Screen` can
be a simple marker object type or a data object with information to pass on.

```kotlin
@Parcelize
data class AddFavoritesScreen(val externalId: UUID) : Screen
```

These are used by `Navigator`s (when called from presenters) or `CircuitContent` (when called from
UIs) to start a new sub-circuit or nested circuit.

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

The information passed into a screen can also be used to interact with the data layer. In the example here,
we are getting the `externalId` from the screen in order to get information back from our repository. 

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