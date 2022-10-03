Screen
======

The core `Screen` interface is this:

```kotlin
interface Screen : Parcelable
```

These types are `Parcelable` for saveability in our backstack and easy deeplinking. A `Screen` can be a simple marker object type or a data object with information to pass on.

```kotlin
@Parcelize data class AddFavorites(val externalId: UUID) : Screen
```

These are used by `Navigator`s (when called from presenters) or `CircuitContent` (when called from UIs) to start a new sub-circuit.

```kotlin
// In a presenter class
fun showAddFavorites() {
  navigator.goTo(
    AddFavorites(
      externalId = uuidGenerator.generate()
    )
  )
}
```
