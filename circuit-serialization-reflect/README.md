# Module circuit-serialization-reflect

A JVM/Android companion to `circuit-serialization` that persists `@Serializable` screens without requiring polymorphic registration. `ReflectiveSerializableCircuitSaver` records each class's fully qualified name in saved state and resolves its serializer reflectively on restore.

## Installation

```kotlin
dependencies {
  implementation("com.slack.circuit:circuit-serialization-reflect:<version>")
}
```

## Usage

Annotate each saved screen or result with `@Serializable`:

```kotlin
@Serializable
data class DetailScreen(val itemId: Long) : Screen

val saver = ReflectiveSerializableCircuitSaver()
val circuit =
  Circuit.Builder()
    .setCircuitSaver(saver)
    // Add presenter and UI factories.
    .build()

CircuitCompositionLocals(circuit) {
  val backStack = rememberSaveableBackStack(root = DetailScreen(itemId = 1))
  val navigator = rememberCircuitNavigator(backStack)
  NavigableCircuitContent(navigator, backStack)
}
```

No `SerializersModule` setup is needed. See the `circuit-serialization` README for the other ways to wire a `CircuitSaver` up.

The reflective saver can also be the last delegate in an ordered composite:

```kotlin
val defaultSaver = rememberDefaultCircuitSaver()
val saver = remember(defaultSaver, serializableSaver) {
  serializableSaver + defaultSaver + ReflectiveSerializableCircuitSaver()
}
```

This prefers registered serialization, then values supported directly by the current registry, and finally reflective serialization.

Use `restoreScreen<T>` and `restorePopResult<T>` to restore a specific type.

## R8/ProGuard

This artifact embeds its required keep rules, so minified apps work without additional
configuration. The embedded rules keep the names of all `CircuitSaveable` implementations (restore
resolves them with `Class.forName`) and their generated kotlinx-serialization serializers. This
means screen and pop-result classes are excluded from name obfuscation.

## Caveats

Restore matches on class names, so renaming or moving a screen class invalidates its previously
saved records. Records that no longer resolve are dropped instead of failing. See the
`circuit-serialization` README for the full lenient-restoration behavior.
