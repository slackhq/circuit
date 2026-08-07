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

Use the `circuit-serialization` artifact to persist navigation state with kotlinx serialization. Android screens and pop results must still be Parcelable. A future release will remove this requirement. The saver stores `SavedState` instead of the Parcelable value.

`@CircuitSerializable` supplies the default kotlinx serializer. Circuit's KSP processor contributes a registration through Metro, Hilt, kotlin-inject-anvil, or Anvil. For example, a Metro graph can declare the registration set and provide the saver like this:

```kotlin
@Parcelize
@CircuitSerializable(AppScope::class)
data object HomeScreen : Screen

@Multibinds
fun circuitSerializerRegistrations(): Set<CircuitSerializerRegistration>

@Provides
fun provideCircuitSaver(
  registrations: Set<CircuitSerializerRegistration>,
): CircuitSaver = SerializableCircuitSaver(registrations)
```

Each Gradle module compiles the generated set contributions for its annotated types. The application graph collects contributions from the application module and its dependency modules in the injected `Set<CircuitSerializerRegistration>`. Apps without a supported DI framework can register `@Serializable` types manually in a `SerializersModule`. They can also use the reflective saver. See the [code generation guide](code-gen.md#serialization-registrations) for setup and generated code.

On JVM and Android, `ReflectiveSerializableCircuitSaver()` can resolve serializers from the saved class name. Apps that use it do not need to register each type. The `circuit-serialization-reflect` artifact includes the R8 and ProGuard rules it needs. Minified apps do not need extra configuration.

Both serializing savers can restore navigation state written by Circuit 0.34's default saver. Switching to serialization does not reset that state.

When a saved value can no longer be restored:

- `SaveableBackStack` drops the affected record. If none survive, it starts from its initial value.
- `SaveableNavStack` discards incomplete forward history. If the active screen or its back history is missing, it starts from its initial value.
- Stored back-stack snapshots are discarded if any record is missing.
- An unrestorable pending pop result clears its expectation, so `awaitResult` returns null.

See the `circuit-serialization` README for the full setup.

To disable persistence entirely, use `CircuitSaver.NoOp`. Stacks saved with it restore to their initial state. This changes persistence behavior only. Android `Screen` and `PopResult` implementations must still be `Parcelable` for now.

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

`Screen` and `PopResult` still extend `Parcelable` on Android. A future release will remove those
supertypes. To prepare, implement `ParcelableScreen` or `ParcelablePopResult` on values that
should keep using Parcelable, or adopt a serializing `CircuitSaver`. The `circuit-serialization`
README has the full roadmap.
