Screen
======

Screens are keys for Presenter and UI pairings.

The core `Screen` interface is this:

```kotlin
interface Screen : Parcelable
```

These types are `Parcelable` on Android for saveability in our backstack and easy deeplinking. A 
`Screen` can be a simple marker `data object` or a `data class` with information to pass on.

```kotlin
@Parcelize
data object HomeScreen : Screen

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

Screens are also used to look up those corresponding components in `Circuit`.

```kotlin
val presenter: Presenter<*>? = circuit.presenter(addFavoritesScreen, navigator)
val ui: Ui<*>? = circuit.ui(addFavoritesScreen)
```

!!! tip "Nomenclature"
    Semantically, in this example we would call all of these components together the "AddFavorites Screen".

## Saving and restoring

Circuit's saveable back stacks (`rememberSaveableBackStack` and `rememberSaveableNavStack`) persist
navigation state across configuration changes and process death. How screens are converted to a
saveable form is pluggable via `CircuitSaver`. Both `Screen` and `PopResult` extend the
`CircuitSaveable` marker, and a `CircuitSaver` converts those values to and from
representations that Compose's `SaveableStateRegistry` can store.

```kotlin
abstract class CircuitSaver protected constructor() {
  abstract fun save(value: CircuitSaveable): Any?

  protected abstract fun restore(saved: Any): CircuitSaveable?
}

inline fun <reified T : Screen> CircuitSaver.restoreScreen(
  saved: Any,
  onAbsent: () -> Unit = {},
  onTypeMismatch: (CircuitSaveable) -> Unit = {
    error("Expected ${T::class}, but CircuitSaver restored ${it::class}.")
  },
): T?

inline fun <reified T : PopResult> CircuitSaver.restorePopResult(
  saved: Any,
  onAbsent: () -> Unit = {},
  onTypeMismatch: (CircuitSaveable) -> Unit = {
    error("Expected ${T::class}, but CircuitSaver restored ${it::class}.")
  },
): T?
```

`restore` is a protected implementation hook for `CircuitSaver` authors. Application code uses the
reified helpers instead.

The reified type parameter is the concrete expected type: `restoreScreen<HomeScreen>(saved)`
rejects another `Screen` subtype. When the saver returns null, the helper invokes `onAbsent` and
returns null. When the restored value is not the requested type, the helper passes it to
`onTypeMismatch`. That callback throws by default; if a custom callback completes normally, the
helper returns null. `restorePopResult` has the same behavior for `PopResult` subtypes.

The default (`DefaultCircuitSaver`) passes values through unchanged. On Android that means screens
persist via their `Parcelable` implementations, matching Circuit's historical behavior. Other
platforms hold saved state in memory only.

### Choosing a strategy

Parcelable is the Android default and needs no setup. Annotate screens with `@Parcelize` and the
default saver persists them. For common-code screens, implement `ParcelableScreen`, which adds
`Parcelable` on Android and is just a `Screen` elsewhere.

To persist `SavedState` encoded with kotlinx-serialization, use the `circuit-serialization`
artifact. In 0.35, Android screens still need `@Parcelize` in addition to `@Serializable`, even
though the saver stores `SavedState` rather than the Parcelable value:

```kotlin
@Parcelize
@Serializable
data object HomeScreen : Screen

val saver = SerializableCircuitSaver(
  SavedStateConfiguration {
    serializersModule = SerializersModule {
      polymorphic(CircuitSaveable::class) {
        subclass(HomeScreen::class)
        // Register every screen and pop result here
      }
    }
  }
)
```

On JVM and Android, `ReflectiveSerializableCircuitSaver()` from the `circuit-serialization-reflect`
artifact skips the registration requirement by resolving serializers reflectively from the saved
class name. The artifact embeds the R8/ProGuard rules it needs, so minified apps work without
additional configuration.

See the `circuit-serialization` README for the full setup.

To disable persistence entirely, use `CircuitSaver.NoOp`. Stacks saved with it restore to their
initial state.

### Wiring

Pass a saver directly to back stack creation, or provide it once at the app root:

```kotlin
// Explicit
val backStack = rememberSaveableBackStack(root = HomeScreen, circuitSaver = saver)

// Or at the root, reaches all back stacks below it
ProvideCircuitSaver(saver) {
  // App content
}
```

`Circuit.Builder.setCircuitSaver(saver)` also provides it via `CircuitCompositionLocals`, reaching
any back stack created inside it.

!!! note "Back stacks created outside `CircuitCompositionLocals`"
    Composition locals only reach content below their provider. If a back stack is created above
    `CircuitCompositionLocals`, a saver set on `Circuit.Builder` won't apply to it. Pass the saver
    explicitly or use `ProvideCircuitSaver` above the back stack creation.

### Roadmap

`Screen`'s Android `actual` still extends `Parcelable`. A future release removes that supertype,
making `Screen` a plain marker interface on every platform. To prepare, implement
`ParcelableScreen` on screens that should keep using Parcelable, or adopt a serializing
`CircuitSaver`. The `circuit-serialization` README has the full roadmap.
