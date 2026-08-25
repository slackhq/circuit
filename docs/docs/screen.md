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
  protected open fun canSave(value: CircuitSaveable): Boolean = false

  public abstract fun save(value: CircuitSaveable): Any?

  protected open fun canRestore(saved: Any): Boolean = false

  protected abstract fun restore(saved: Any): CircuitSaveable?

  companion object {
    val NoOp: CircuitSaver
    fun Dropping(onDropped: (CircuitSaveable) -> Unit): CircuitSaver
  }
}

operator fun CircuitSaver.plus(other: CircuitSaver): CircuitSaver

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

`canSave` and `canRestore` are protected routing hooks for ordered composites built with `+`. They default to false, so custom savers override them when they should participate in a composite. The first saver that claims a value owns the operation. Its result is final even when it returns null or throws. Saving through a composite throws when no saver claims the value. Append `CircuitSaver.Dropping { }` when unmatched values should be dropped instead, using its callback to observe each drop. Restoration returns null when no saver claims the saved value, since saved data can come from an older app version and should degrade rather than crash.

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

### Registry-backed persistence

`rememberDefaultCircuitSaver()` captures the nearest Compose `SaveableStateRegistry` and passes a screen or result through only when that registry accepts the value. The captured registry only decides which values the saver handles. Each stack's `rememberSaveable` call still registers with the registry at the stack's call site. Standard saveable-state holder child registries delegate this check to their parent. If a custom nested registry accepts different values, create and pass a saver from that registry's scope.

On Android, the registry accepts values that can be stored in a `Bundle`. `Parcelable` is one option. For common-code values, annotate the class with `@Parcelize` and implement `ParcelableScreen` or `ParcelablePopResult`. These interfaces add `Parcelable` on Android and remain plain `Screen` or `PopResult` subtypes elsewhere.

```kotlin
@Parcelize
data object HomeScreen : ParcelableScreen
```

`@Serializable` does not make an object directly acceptable to the platform registry. Use `SerializableCircuitSaver` or `ReflectiveSerializableCircuitSaver` to persist those values.

When used directly, the saver fails the save if there is no registry or the registry rejects a value. Combine it with `CircuitSaver.Dropping` to drop unsupported values instead and optionally report them:

```kotlin
val defaultSaver = rememberDefaultCircuitSaver()
val circuitSaver = remember(defaultSaver) {
  defaultSaver + CircuitSaver.Dropping { log.warn("dropping ${it::class}") }
}
```

If no records survive restoration, Circuit creates the stack again from its initial value.

### Combining savers

Use `+` to try multiple persistence strategies in order. Registered serialization should come first so registered types use that representation. The registry-backed saver can handle values supported directly by the current registry, with reflection as a JVM and Android fallback.

```kotlin
val defaultSaver = rememberDefaultCircuitSaver()
val circuitSaver =
  remember(defaultSaver, serializableCircuitSaver) {
    serializableCircuitSaver +
      defaultSaver +
      ReflectiveSerializableCircuitSaver()
  }
```

Omit the reflective saver on platforms where `circuit-serialization-reflect` is unavailable. Existing raw, registered, and reflective saved values remain readable. If more than one saver recognizes a saved value, the earlier saver restores it.

### No persistence

To disable persistence entirely, use `CircuitSaver.NoOp`. Stacks saved with it restore to their initial state only. Screens and results used with it do not need a persistence format.

### Providing the saver

`CircuitCompositionLocals(circuit)` uses a static saver configured on `Circuit` when one is present. Otherwise, it chooses a saver inherited from an outer `ProvideCircuitSaver` or creates a registry-backed fallback with `rememberDefaultCircuitSaver()`, then applies any saver transform configured on `Circuit`. Stacks created inside those locals inherit the result:

```kotlin
CircuitCompositionLocals(circuit) {
  val backStack = rememberSaveableBackStack(root = HomeScreen)
  val navigator = rememberCircuitNavigator(backStack)
  NavigableCircuitContent(navigator, backStack)
}
```

`ProvideCircuitSaver` can provide a saver when `CircuitCompositionLocals` is not the right scope:

```kotlin
ProvideCircuitSaver(circuitSaver) {
  val backStack = rememberSaveableBackStack(HomeScreen)
  // App content
}
```

The saveable stack and result-handler remember functions also accept a `CircuitSaver` parameter. Pass it when one instance needs to use a different saver or is created outside either provider.

`Circuit.Builder.setCircuitSaver(saver)` stores an app-configured saver on `Circuit`. `newBuilder()` inherits it, and callers can replace or clear it. The no-saver `CircuitCompositionLocals(circuit)` overload uses this property automatically.

The transform overload receives the inherited or registry-backed fallback saver, so the final composite can also remain configured on `Circuit`:

```kotlin
Circuit.Builder()
  .setCircuitSaver { fallbackSaver ->
    serializableCircuitSaver + fallbackSaver
  }
  .build()
```

Fresh equivalent saver instances are safe across recreation because restoration routes from the saved representation. The transform must preserve compatible saved formats, registrations, and delegate order. Do not use transient state or feature flags to add, remove, or reorder savers.

!!! note "Composition-built savers"
    A composition-built saver can also be passed to the explicit `CircuitCompositionLocals` overload, which takes precedence over any saver configured on the `Circuit`. Create the stack inside those locals so it uses the same saver:

    ```kotlin
    CircuitCompositionLocals(circuit, circuitSaver) {
      val backStack = rememberSaveableBackStack(root = HomeScreen)
      val navigator = rememberCircuitNavigator(backStack)
      NavigableCircuitContent(navigator, backStack)
    }
    ```

### Migrating from Circuit 0.36 and earlier

`Screen` and `PopResult` no longer extend `Parcelable` on Android. Choose a persistence strategy for every saveable back stack:

- To use kotlinx-serialization, configure one of Circuit's serializing savers and remove `@Parcelize` from the types it saves.
- To use Android's native Parcelable persistence, retain `@Parcelize`, change `Screen` to `ParcelableScreen` or `PopResult` to `ParcelablePopResult`, and use `rememberDefaultCircuitSaver()`.
- To disable navigation persistence, use `CircuitSaver.NoOp` with plain `Screen` and `PopResult` types.

### Quick reference

| Persistence                                             | Screen or result declaration                                  | Saver setup                                                        |
|---------------------------------------------------------|---------------------------------------------------------------|--------------------------------------------------------------------|
| kotlinx-serialization with reflection on JVM or Android | `@Serializable`                                               | `ReflectiveSerializableCircuitSaver()`                             |
| kotlinx-serialization with generated DI registration    | `@CircuitSerializable(AppScope::class)`                       | Inject the generated registrations into `SerializableCircuitSaver` |
| kotlinx-serialization with manual registration          | `@Serializable`                                               | Register each type with `SerializableCircuitSaver`                 |
| Platform registry, including Android Parcelable values | A type accepted by the current registry                        | `rememberDefaultCircuitSaver()`                                    |
| No navigation persistence                               | Plain `Screen` or `PopResult`                                 | `CircuitSaver.NoOp`                                                |
