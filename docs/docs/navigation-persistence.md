Saving navigation state
=======================

`rememberSaveableBackStack` and `rememberSaveableNavStack` preserve navigation state through Compose's `SaveableStateRegistry`, including Android configuration changes and process recreation. Both `Screen` and `PopResult` extend the `CircuitSaveable` marker interface. A `CircuitSaver` converts those values into representations that the registry can store.

Circuit uses kotlinx-serialization as its standard persistence strategy, but you can use the platform registry directly or disable navigation persistence.

| Strategy                                                | Screen or result declaration            | Saver setup                                                                                                   |
|---------------------------------------------------------|-----------------------------------------|---------------------------------------------------------------------------------------------------------------|
| kotlinx-serialization with reflection on JVM or Android | `@Serializable`                         | `ReflectiveSerializableCircuitSaver()`                                                                        |
| kotlinx-serialization with generated DI registration    | `@CircuitSerializable(AppScope::class)` | Inject the generated registrations into `SerializableCircuitSaver`                                            |
| kotlinx-serialization with manual registration          | `@Serializable`                         | Register each type with `SerializableCircuitSaver`                                                            |
| Platform registry, including Android Parcelable values  | A type accepted by the current registry | Automatic in `CircuitCompositionLocals`. Call `rememberDefaultCircuitSaver()` only for explicit provisioning. |
| No navigation persistence                               | Plain `Screen` or `PopResult`           | `CircuitSaver.NoOp`                                                                                           |

## kotlinx-serialization

Annotate each saved screen and result with `@Serializable`.

On JVM and Android, `ReflectiveSerializableCircuitSaver` finds the serializer from the saved class name, so you do not need to register each type:

```kotlin
@Serializable
data object HomeScreen : Screen

val circuit =
  Circuit.Builder()
    .setCircuitSaver(ReflectiveSerializableCircuitSaver())
    .build()
```

The `circuit-serialization-reflect` artifact includes the R8 and ProGuard rules it needs. Minified apps do not need extra configuration. See its [README](https://github.com/slackhq/circuit/tree/main/circuit-serialization-reflect) for installation and usage.

Apps using Circuit code generation with Metro, Hilt, kotlin-inject-anvil, or Anvil can replace `@Serializable` with `@CircuitSerializable`:

```kotlin
@CircuitSerializable(AppScope::class)
data object HomeScreen : Screen
```

`@CircuitSerializable` supplies the default kotlinx serializer and generates a registration for the concrete type. The selected DI framework contributes that registration to the set used by `SerializableCircuitSaver`. See the [code generation guide](code-gen.md#serialization-registrations) for installation, generated code, and DI setup.

Kotlin Multiplatform apps without a supported DI framework can register their `@Serializable` types manually with `SerializableCircuitSaver`. See the [`circuit-serialization` README](https://github.com/slackhq/circuit/tree/main/circuit-serialization) for the complete setup.

## Registry-backed persistence

`CircuitCompositionLocals(circuit)` creates a registry-backed fallback when no static or inherited saver is available. Call `rememberDefaultCircuitSaver()` when you need to pass or combine that saver explicitly. It uses the Compose `SaveableStateRegistry` in its current composition scope and accepts only values that registry can save.

On Android, the registry accepts values that can be stored in a `Bundle`. `Parcelable` is one option. For common-code values, annotate the class with `@Parcelize` and implement `ParcelableScreen` or `ParcelablePopResult`. These interfaces add `Parcelable` on Android and remain plain `Screen` or `PopResult` subtypes elsewhere.

```kotlin
@Parcelize
data object HomeScreen : ParcelableScreen
```

`@Serializable` does not make an object directly acceptable to the platform registry. Use `SerializableCircuitSaver` or `ReflectiveSerializableCircuitSaver` to persist those values.

Create an explicit default saver in the same saveable-state scope as the stack. Standard `SaveableStateHolder` scopes work automatically. A custom nested registry with different acceptance rules can create and pass its own saver.

## Combining persistence strategies

Use `+` to try multiple savers in order. Registered serialization should come first so registered types use that representation. The registry-backed saver can handle values supported directly by the current registry, with reflection as a JVM and Android fallback.

```kotlin
val reflectiveSaver = ReflectiveSerializableCircuitSaver()
val circuit =
  Circuit.Builder()
    .setCircuitSaver { fallbackSaver ->
      serializableCircuitSaver + fallbackSaver + reflectiveSaver
    }
    .build()
```

Omit the reflective saver on platforms where `circuit-serialization-reflect` is unavailable. Existing raw, registered, and reflective saved values remain readable. If more than one saver recognizes a saved value, the earlier saver restores it.

The first saver that claims a value owns the operation. Its result is final even when it returns null or throws. Saving fails when no saver claims a value. Append `CircuitSaver.Dropping { value -> ... }` to drop unmatched values and optionally report them. Restoration returns null when no saver claims the saved value because saved data can come from an older app version.

## Disabling persistence

Use `CircuitSaver.NoOp` to disable navigation persistence. Stacks saved with it restore to their initial state. Screens and results used with it do not need a persistence format.

## Providing the saver

`CircuitCompositionLocals(circuit)` uses a static saver configured on `Circuit` when one is present. Otherwise, it chooses a saver inherited from an outer `ProvideCircuitSaver` or creates a registry-backed fallback with `rememberDefaultCircuitSaver()`. It then applies any saver transform configured on `Circuit`. Stacks created inside those locals inherit the result:

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

`Circuit.Builder.setCircuitSaver(saver)` stores an app-configured saver on `Circuit`. `newBuilder()` inherits it, and callers can replace or clear it. `CircuitCompositionLocals(circuit)` uses this property automatically.

The transform overload receives the inherited or registry-backed fallback saver, which makes it useful for the mixed strategy shown above. The transform may produce a new saver after recreation, but it must keep compatible saved formats, registrations, and delegate order. Do not use transient state or feature flags to change the saver chain.

A composition-built saver can instead be passed to the explicit `CircuitCompositionLocals` overload. This takes precedence over any saver configured on `Circuit`. Create the stack inside those locals so it uses the same saver:

```kotlin
CircuitCompositionLocals(circuit, circuitSaver) {
  val backStack = rememberSaveableBackStack(root = HomeScreen)
  val navigator = rememberCircuitNavigator(backStack)
  NavigableCircuitContent(navigator, backStack)
}
```

## Restoration behavior

When a saved screen or result can no longer be restored:

- `SaveableBackStack` drops the affected record. If none survive, it starts from its initial value.
- `SaveableNavStack` discards incomplete forward history. If the active screen or its back history is missing, it starts from its initial value.
- Stored back-stack snapshots are discarded if any record is missing.
- An unrestorable pending pop result clears its expectation, so `awaitResult` returns null.

## Custom savers

Most applications can use one of the savers above. A custom saver implements the following contract:

```kotlin
abstract class CircuitSaver protected constructor() {
  protected open fun canSave(value: CircuitSaveable): Boolean = false

  public abstract fun save(value: CircuitSaveable): Any?

  protected open fun canRestore(saved: Any): Boolean = false

  protected abstract fun restore(saved: Any): CircuitSaveable?
}
```

`canSave` and `canRestore` are routing hooks for ordered composites built with `+`. They default to false, so a custom saver must override them to participate in a composite.

`restore` is a protected implementation hook. Application code uses the typed `restoreScreen` and `restorePopResult` helpers. Their reified type parameter is the concrete expected type, so `restoreScreen<HomeScreen>(saved)` rejects another `Screen` subtype. The helpers return null when the saver cannot restore the value and fail by default when it restores the wrong type. Their callbacks can customize either case.

## Migrating from Circuit 0.37 and earlier

`Screen` and `PopResult` no longer extend `Parcelable` on Android. Choose a persistence strategy for every saveable navigation stack:

- If you already use `SerializableCircuitSaver` or `ReflectiveSerializableCircuitSaver`, remove `@Parcelize`, use `Screen` or `PopResult` directly, and keep the serializing saver.
- To keep Parcelable persistence, retain `@Parcelize`, change `Screen` to `ParcelableScreen` or `PopResult` to `ParcelablePopResult`, and use the automatic registry-backed fallback.
- To migrate gradually, put the serializing saver before the registry-backed fallback with `setCircuitSaver { fallbackSaver -> serializableCircuitSaver + fallbackSaver }`. Converted types use serialization while remaining Parcelable types continue using the platform registry.
- To disable navigation persistence, configure `CircuitSaver.NoOp`. Plain `Screen` and `PopResult` types need no persistence annotations.

Remove references to `DefaultCircuitSaver`. `CircuitCompositionLocals(circuit)` creates the registry-backed fallback automatically. When a stack is created outside those locals, call `rememberDefaultCircuitSaver()` and pass it to the stack or provide it with `ProvideCircuitSaver`.

The deprecated no-argument `SaveableBackStack.Saver` and `SaveableNavStack.Saver` properties have also been removed. Low-level callers must pass a `CircuitSaver` to the corresponding `Saver(circuitSaver)` function.

Unsupported values now fail when saved instead of being silently dropped. The [combining strategies](#combining-persistence-strategies) section explains how to opt into dropping them.
