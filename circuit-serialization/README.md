# Module circuit-serialization

kotlinx-serialization support for persisting Circuit navigation state. This artifact provides `CircuitSaver` implementations that encode `Screen`s and `PopResult`s to `SavedState` with `androidx.savedstate`. Saveable back stacks can survive configuration changes and process death without storing Parcelable values.

## Installation

```kotlin
dependencies {
  implementation("com.slack.circuit:circuit-serialization:<version>")
}
```

Apply the kotlinx-serialization compiler plugin:

```kotlin
plugins {
  kotlin("plugin.serialization")
}
```

## SerializableCircuitSaver

`SerializableCircuitSaver` works on all platforms. `@CircuitSerializable` supplies a default kotlinx serializer. The serialization processor in `circuit-codegen` generates a polymorphic registration for each annotated screen and result and contributes it to your DI graph. Add the processor alongside your existing Metro, Hilt, kotlin-inject-anvil, or Anvil setup:

```kotlin
dependencies {
  ksp("com.slack.circuit:circuit-codegen:<version>")
}
```

For a multiplatform project using Metro or kotlin-inject-anvil, add the processor to `kspCommonMainMetadata`. Also add it to each target-specific KSP configuration that compiles the annotated declarations. Anvil and Hilt support code generation only for JVM and Android targets.

Annotate each saved type with the same DI scope used by your Circuit graph. `@CircuitSerializable` also supplies the kotlinx serializer, so a separate `@Serializable` annotation is not required:

```kotlin
@CircuitSerializable(AppScope::class)
data object HomeScreen : Screen

@CircuitSerializable(AppScope::class)
data class DetailScreen(val itemId: Long) : Screen

@CircuitSerializable(AppScope::class)
data class DetailResult(val itemId: Long) : PopResult
```

The processor contributes a `CircuitSerializerRegistration` for each annotated type. For example, a Metro graph can declare the registration set and provide the saver like this:

```kotlin
@Multibinds
fun circuitSerializerRegistrations(): Set<CircuitSerializerRegistration>

@Provides
fun provideCircuitSaver(
  registrations: Set<CircuitSerializerRegistration>,
): CircuitSaver = SerializableCircuitSaver(registrations)
```

Each Gradle module compiles the generated set contributions for its annotated types. The application graph collects contributions from the application module and its dependency modules in the injected `Set<CircuitSerializerRegistration>`. Serialization code generation uses the same `circuit.codegen.mode` setting as `@CircuitInject`. See the code generation guide for mode-specific setup and generated code examples.

For an expect/actual screen or result, annotate the `expect` declaration and every `actual` declaration with `@CircuitSerializable` using the same scope. The annotation supplies the default serializer in every compilation. The processor generates one registration from the `expect` declaration.

To use a custom serializer for a screen or result, keep `@CircuitSerializable` for registration and add `@Serializable(with = ...)`:

```kotlin
@CircuitSerializable(AppScope::class)
@Serializable(with = LegacyScreenSerializer::class)
data class LegacyScreen(val value: String) : Screen
```

`SerializableCircuitSaver(registrations, configuration)` adds generated registrations to the configuration's existing serializers module. It preserves the other configuration options. Conflicting registrations fail when the saver is created.

Registrations against `CircuitSaveable` are also used for nested properties declared as `Screen` or `PopResult`.

### Manual registration

Apps that do not use DI can register `@Serializable` screens and results manually against the `CircuitSaveable` base class in a `SavedStateConfiguration`:

```kotlin
@Serializable
data object HomeScreen : Screen

@Serializable
data class DetailScreen(val itemId: Long) : Screen

val saver = SerializableCircuitSaver(
  SavedStateConfiguration {
    serializersModule = SerializersModule {
      polymorphic(CircuitSaveable::class) {
        subclass(HomeScreen::class)
        subclass(DetailScreen::class)
      }
    }
  }
)
```

Saving an unregistered type fails with a descriptive error. Restoring an unregistered type returns null, allowing the navigation owner to drop that record. Pass an `onRestoreError` callback to observe restoration failures.

Use `restoreScreen<T>` and `restorePopResult<T>` to restore a specific type. They return null when the saver cannot restore a value. By default, they reject a different concrete `Screen` or `PopResult` type.

## Skipping registration on JVM/Android

The `circuit-serialization-reflect` artifact provides `ReflectiveSerializableCircuitSaver`. It resolves serializers from the saved class name, so apps do not need to register each type. The artifact includes the R8 and ProGuard rules it needs. Minified apps do not need extra configuration. See its README for details.

## Wiring it up

Store the saver on `Circuit`. `CircuitCompositionLocals(circuit)` provides it to stacks created inside its content:

```kotlin
val circuit =
  Circuit.Builder()
    .setCircuitSaver(saver)
    // Add presenter and UI factories.
    .build()

CircuitCompositionLocals(circuit) {
  val backStack = rememberSaveableBackStack(root = HomeScreen)
  val navigator = rememberCircuitNavigator(backStack)
  NavigableCircuitContent(navigator, backStack)
}
```

`ProvideCircuitSaver` can provide the same local in another scope. `CircuitCompositionLocals(circuit)` inherits it when the `Circuit` has no configured saver:

```kotlin
ProvideCircuitSaver(saver) {
  CircuitCompositionLocals(circuit) {
    // App content
  }
}
```

`CircuitCompositionLocals(circuit)` uses a static saver configured on `Circuit` when one is present. Otherwise, it chooses an inherited `LocalCircuitSaver` or a registry-backed saver as the fallback, then applies any configured saver transform. Its overload that accepts a saver takes precedence. Create saveable stacks inside those locals, or pass the saver directly when a stack is created outside them.

### Combining persistence strategies

Use `+` to route values through multiple savers in order. For example, an Android or JVM app can prefer generated registrations, then values supported directly by the current `SaveableStateRegistry`, then reflective serialization:

```kotlin
val defaultSaver = rememberDefaultCircuitSaver()
val saver =
  remember(defaultSaver, serializableSaver) {
    serializableSaver + defaultSaver + ReflectiveSerializableCircuitSaver()
  }
```

The first saver that claims a value owns the operation. Its null result or failure is final. Append `CircuitSaver.Dropping { value -> ... }` to drop values that none of the earlier savers support and optionally report them.

The default saver captures the nearest registry where it is created. If a custom nested registry accepts different values, create and pass a saver from that registry's scope.

## Lenient restoration

When a saved value can no longer be restored:

- `SaveableBackStack` drops the affected record. If none survive, it starts from its initial value.
- `SaveableNavStack` discards incomplete forward history. If the active screen or its back history is missing, it starts from its initial value.
- Stored back-stack snapshots are discarded if any record is missing.
- An unrestorable pending pop result clears its expectation, so `awaitResult` returns null.
