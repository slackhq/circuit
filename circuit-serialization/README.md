# Module circuit-serialization

kotlinx-serialization support for persisting Circuit navigation state. This artifact provides
`CircuitSaver` implementations that encode `Screen`s and `PopResult`s to `SavedState` via
`androidx.savedstate`, so saveable back stacks survive configuration changes and process death
without requiring `Parcelable`.

## Installation

```kotlin
dependencies {
  implementation("com.slack.circuit:circuit-serialization:<version>")
}
```

Screens must be annotated with `@Serializable`, which requires the kotlinx-serialization compiler
plugin:

```kotlin
plugins {
  kotlin("plugin.serialization")
}
```

## SerializableCircuitSaver

`SerializableCircuitSaver` works on all platforms. Screens and results are registered for
polymorphic serialization against the `CircuitSaveable` base class in a `SavedStateConfiguration`:

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

Saving an unregistered type fails with a descriptive error. Restoring an unregistered type, such as
after an app update removed a screen, drops that record instead of failing. Pass an
`onRestoreError` callback to observe dropped records, such as for logging.

## Skipping registration on JVM/Android

The `circuit-serialization-reflect` artifact provides `ReflectiveSerializableCircuitSaver`, which
skips the registration requirement by resolving serializers reflectively from the saved class
name. It embeds the R8/ProGuard rules it needs, so it works in minified apps without additional
configuration. See its README for details.

## Wiring it up

Pass the saver to back stack creation directly:

```kotlin
val backStack = rememberSaveableBackStack(root = HomeScreen, circuitSaver = saver)
```

Or provide it once at the app root so every back stack picks it up:

```kotlin
ProvideCircuitSaver(saver) {
  // App content
}
```

`Circuit.Builder.setCircuitSaver(saver)` also provides it via `CircuitCompositionLocals`. Note
this only reaches back stacks created inside `CircuitCompositionLocals`. A back stack created
above it needs one of the other two options.

## Roadmap

`Screen`'s Android `actual` currently extends `Parcelable`, so Android screens must still be
parcelable even when a serializing saver handles persistence. A future release removes that
supertype and completes the migration:

- `Screen` becomes a plain marker interface on all platforms, and `@Parcelize` becomes optional
  for apps that use a saver from this artifact.
- The default Android saver keeps working for screens that implement `Parcelable` and fails with
  a descriptive error for screens that don't when no saver is configured.
- Common-code screens that should keep the Parcelable strategy migrate to `ParcelableScreen`,
  which adds `Parcelable` on Android only.

Planned follow-ups after that:

- KSP code generation for the `polymorphic(CircuitSaveable::class)` registrations, replacing the
  hand-written `SerializersModule`.
- Removal of the deprecated `SaveableBackStack.Record.args` and the deprecated companion `Saver`
  vals.
