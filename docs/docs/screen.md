Screen
======

Screens are keys for Presenter and UI pairings.

The core `Screen` interface is this:

```kotlin
interface Screen
```

`Screen` does not require a particular persistence format. A `Screen` can be a simple marker `data object` or a `data class` with information to pass on.

```kotlin
@Serializable
data object HomeScreen : Screen

@Serializable
data class AddFavoritesScreen(val externalId: UUID) : Screen
```

Circuit's documentation uses kotlinx-serialization for screens and results by default. The annotation supplies a serializer, but the app must also configure a serializing `CircuitSaver` as described below.

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

### kotlinx-serialization

Circuit uses kotlinx-serialization as the standard persistence strategy. Most examples use `@Serializable` and assume the app has configured a serializing `CircuitSaver` at its root.

#### Using `@Serializable`

Annotate each saved screen and result with `@Serializable`.

On JVM and Android, `ReflectiveSerializableCircuitSaver()` finds these serializers from the saved class name, so you do not need to register each type:

```kotlin
@Serializable
data object HomeScreen : Screen

val circuitSaver = ReflectiveSerializableCircuitSaver()
```

The `circuit-serialization-reflect` artifact includes the R8 and ProGuard rules it needs. Minified apps do not need extra configuration.

Kotlin Multiplatform apps that do _not_ use DI can register their `@Serializable` types manually with `SerializableCircuitSaver`. See the `circuit-serialization` README for the full setup.

#### Using `@CircuitSerializable` with DI

Apps using Circuit code generation with Metro, Hilt, kotlin-inject-anvil, or Anvil should replace `@Serializable` with `@CircuitSerializable`.

`@CircuitSerializable` is a _meta-annotation_ (via `@MetaSerializable`) for `@Serializable` that works for both kotlinx-serialization and Circuit's serializer code generation.

Circuit code gen generates a `CircuitSerializerRegistration` for each annotated screen/result and contributes it to the specified DI scope. The application graph collects those registrations into a set, which `SerializableCircuitSaver` uses to register each concrete type for polymorphic `CircuitSaveable` serialization.

For example, a Metro graph can collect the generated registrations and provide `SerializableCircuitSaver`:

```kotlin
@CircuitSerializable(AppScope::class)
data object HomeScreen : Screen

@Multibinds
fun circuitSerializerRegistrations(): Set<CircuitSerializerRegistration>

@Provides
fun provideCircuitSaver(
  registrations: Set<CircuitSerializerRegistration>,
): CircuitSaver = SerializableCircuitSaver(registrations)
```

Each Gradle module compiles the generated set contributions for its annotated types. The application graph collects contributions from the application module and its dependency modules in the injected `Set<CircuitSerializerRegistration>`. See the [code generation guide](code-gen.md#serialization-registrations) for setup and generated code.

When a saved value can no longer be restored:

- `SaveableBackStack` drops the affected record. If none survive, it starts from its initial value.
- `SaveableNavStack` discards incomplete forward history. If the active screen or its back history is missing, it starts from its initial value.
- Stored back-stack snapshots are discarded if any record is missing.
- An unrestorable pending pop result clears its expectation, so `awaitResult` returns null.

### Android Parcelable option

Apps that choose Android's `DefaultCircuitSaver` can use `Parcelable`. For common-code values, annotate the class with `@Parcelize` and implement `ParcelableScreen` or `ParcelablePopResult`. These interfaces add `Parcelable` on Android and remain plain `Screen` or `PopResult` subtypes elsewhere.

```kotlin
@Parcelize
data object HomeScreen : ParcelableScreen
```

On Android, `DefaultCircuitSaver` saves only `Parcelable` screens and results. Non-`Parcelable` records do not survive activity recreation or process death. If no records survive, Circuit recreates the stack from its initial value.

On other platforms, `DefaultCircuitSaver` passes values through unchanged.

### No persistence

To disable persistence entirely, use `CircuitSaver.NoOp`. Stacks saved with it restore to their initial state only. Screens and results used with it do not need a persistence format.

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

### Migrating from Circuit 0.36 and earlier

`Screen` and `PopResult` no longer extend `Parcelable` on Android. Choose a persistence strategy for every saveable back stack:

- To use kotlinx-serialization, configure one of Circuit's serializing savers and remove `@Parcelize` from the types it saves.
- To keep the Android default saver, retain `@Parcelize` and change `Screen` to `ParcelableScreen` or `PopResult` to `ParcelablePopResult`.
- To disable navigation persistence, use `CircuitSaver.NoOp` with plain `Screen` and `PopResult` types.

### Quick reference

| Persistence                                             | Screen or result declaration                                  | Saver setup                                                        |
|---------------------------------------------------------|---------------------------------------------------------------|--------------------------------------------------------------------|
| kotlinx-serialization with reflection on JVM or Android | `@Serializable`                                               | `ReflectiveSerializableCircuitSaver()`                             |
| kotlinx-serialization with generated DI registration    | `@CircuitSerializable(AppScope::class)`                       | Inject the generated registrations into `SerializableCircuitSaver` |
| kotlinx-serialization with manual registration          | `@Serializable`                                               | Register each type with `SerializableCircuitSaver`                 |
| Android Parcelable option                               | `@Parcelize` with `ParcelableScreen` or `ParcelablePopResult` | `DefaultCircuitSaver`                                              |
| No navigation persistence                               | Plain `Screen` or `PopResult`                                 | `CircuitSaver.NoOp`                                                |
