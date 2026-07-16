# Module circuit-serialization-reflect

A JVM/Android companion to `circuit-serialization` that persists `@Serializable` screens without
requiring polymorphic registration. `ReflectiveSerializableCircuitSaver` records each class's
fully qualified name in saved state and resolves its serializer reflectively on restore.

## Installation

```kotlin
dependencies {
  implementation("com.slack.circuit:circuit-serialization-reflect:<version>")
}
```

## Usage

Android screens must still be Parcelable in 0.35, typically via `@Parcelize`, even though this
saver stores serialized `SavedState`. JVM-only screens do not need `@Parcelize`.

```kotlin
@Parcelize // Omit in JVM-only projects
@Serializable
data class DetailScreen(val itemId: Long) : Screen

val saver = ReflectiveSerializableCircuitSaver()

val backStack = rememberSaveableBackStack(root = DetailScreen(itemId = 1), circuitSaver = saver)
```

No `SerializersModule` setup is needed. See the `circuit-serialization` README for the other ways
to wire a `CircuitSaver` up.

Use `restoreScreen<T>` and `restorePopResult<T>` to restore a specific type. The saver can also
restore navigation state saved by Circuit 0.34's default saver, so adopting it in 0.35 does not by
itself reset existing navigation state.

## R8/ProGuard

This artifact embeds its required keep rules, so minified apps work without additional
configuration. The embedded rules keep the names of all `CircuitSaveable` implementations (restore
resolves them with `Class.forName`) and their generated kotlinx-serialization serializers. This
means screen and pop-result classes are excluded from name obfuscation.

## Caveats

Restore matches on class names, so renaming or moving a screen class invalidates its previously
saved records. Records that no longer resolve are dropped instead of failing. See the
`circuit-serialization` README for the full lenient-restoration behavior.

Android `Screen` and `PopResult` implementations still need to be Parcelable in 0.35, even when
using this saver or `CircuitSaver.NoOp`. The Parcelable supertypes are removed in a later release.
