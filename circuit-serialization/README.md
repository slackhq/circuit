# Module circuit-serialization

kotlinx-serialization support for persisting Circuit navigation state. This artifact provides
`CircuitSaver` implementations that encode `Screen`s and `PopResult`s to `SavedState` via
`androidx.savedstate`, so saveable back stacks survive configuration changes and process death
without using Parcelable as the persisted representation. In 0.35, Android screens and results
must still be Parcelable; that type requirement is removed in a future release.

## Installation

```kotlin
dependencies {
  implementation("com.slack.circuit:circuit-serialization:<version>")
}
```

Screens must be annotated with `@Serializable`, which requires the kotlinx-serialization compiler
plugin. Android screens must also remain Parcelable in 0.35, typically via `@Parcelize`:

```kotlin
plugins {
  kotlin("plugin.serialization")
  kotlin("plugin.parcelize") // Android projects in 0.35
}
```

## SerializableCircuitSaver

`SerializableCircuitSaver` works on all platforms. Screens and results are registered for
polymorphic serialization against the `CircuitSaveable` base class in a `SavedStateConfiguration`:

```kotlin
@Parcelize
@Serializable
data object HomeScreen : Screen

@Parcelize
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

Use `restoreScreen<T>` and `restorePopResult<T>` to restore a specific type. They return null when
the saver cannot restore a value and reject a different concrete Screen or PopResult type by
default.

Both serializing savers can restore navigation state saved by Circuit 0.34's default saver. This
allows an app to adopt serialization in 0.35 without resetting existing navigation state.

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

## Lenient restoration

When a screen can no longer be restored, Circuit drops its record. If the active record survives,
it remains active. Otherwise, Circuit selects the nearest surviving record toward the root, then
falls back toward the top. Saved stack snapshots are discarded when their original root cannot be
restored, rather than being associated with a surviving child. If a pending pop result existed but
cannot be restored, Circuit clears the expectation so `awaitResult` returns null rather than
suspending indefinitely.

## Roadmap

`Screen` and `PopResult` currently extend `Parcelable` on Android, so Android implementations must
still be Parcelable even when a serializing saver or `CircuitSaver.NoOp` handles persistence. A
future release removes those supertypes and completes the migration:

- `Screen` and `PopResult` become plain marker interfaces on all platforms, and `@Parcelize`
  becomes optional for apps that use a saver from this artifact.
- The default Android saver keeps working for screens and results that implement `Parcelable` and
  fails with a descriptive error for values that do not when no saver is configured.
- Common-code values that should keep the Parcelable strategy migrate to `ParcelableScreen` or
  `ParcelablePopResult`, which add `Parcelable` on Android only.

Planned follow-ups after that:

- KSP code generation for the `polymorphic(CircuitSaveable::class)` registrations, replacing the
  hand-written `SerializersModule`.
- Removal of the deprecated `SaveableBackStack.Record.args` and the deprecated companion `Saver`
  vals.
