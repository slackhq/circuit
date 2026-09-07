# Module circuit-serialization-reflect

A JVM/Android companion to `circuit-serialization` that persists `@Serializable` screens and pop results without requiring polymorphic registration. `ReflectiveSerializableCircuitSaver` records each class's fully qualified name in saved state and resolves its serializer reflectively on restore.

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

No `SerializersModule` setup is needed. See the [`circuit-serialization` README](https://github.com/slackhq/circuit/tree/main/circuit-serialization) for registered serialization, or [Saving navigation state](https://slackhq.github.io/circuit/docs/navigation-persistence/) for saver selection, composition, and provisioning.

Use `restoreScreen<T>` and `restorePopResult<T>` to restore a specific type.

## R8/ProGuard

This artifact embeds its required keep rules, so minified apps work without additional configuration. The embedded rules keep the names of all `CircuitSaveable` implementations because restore resolves them with `Class.forName`. They also keep the generated kotlinx-serialization serializers. Screen and pop-result classes are therefore excluded from name obfuscation.

## Caveats

Restore matches on class names, so renaming or moving a screen or pop-result class invalidates its previously saved records. Records that no longer resolve are dropped instead of failing. See the [navigation persistence guide](https://slackhq.github.io/circuit/docs/navigation-persistence/) for the full restoration behavior.
