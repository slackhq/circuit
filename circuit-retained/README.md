# Module circuit-retained

This optional artifact contains alternative implementations of `rememberRetained`, `produceRetainedState`, `collectAsRetainedState()`, etc. This is
useful for cases where you want to retain non-saveable state across configuration changes or across a back stack. This
comes at the cost of not participating in the `SavedStateRegistry` and thus not being able to persist across process death, but added flexibility of not
requiring Saveable values. For values that should also survive process death, `rememberRetainedSaveable` layers an opportunistic `Saver` on top of retention.

## Installation

This is automatically set up and available when you use it on available platforms when you use `CircuitCompositionLocals {}`.

If using `NavigableCircuitContent`, a `RetainedStateRegistry` is set up for each back stack record automatically as well.

### Advanced usage

By default, `LocalRetainedStateRegistry` composition local will use a no-op instance. You can provide custom implementations of this to tie into whatever
lifecycle is relevant for your app (back stack, hierarchical, etc). The platform default implementations are accessible via `lifecycleRetainedStateRegistry()`.

```kotlin
CompositionLocalProvider(
  LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry(),
) {
  // Content
}
```

## First-party `retain` interop

To use Compose's `retain` implementation for Circuit's retained APIs, set `CircuitRetainedSettings.useFirstParty = true` before the first composition. Existing Circuit calls do not need to change. Calls to `retain {}` inside `NavigableCircuitContent` follow the lifetime of their navigation record.

Android handles this automatically. On other platforms, wrap your app in `RetainedValuesStoreProvider` so retained values survive composition recreation.

### Compose Desktop Example

Keep a `RetainedValuesStoreOwner` outside the composition and dispose it when the application closes:

```kotlin
fun main() {
  CircuitRetainedSettings.useFirstParty = true
  // Deliberately application-scoped. The owner must outlive every window composition
  // that should share retained values.
  val retainedValuesStoreOwner = RetainedValuesStoreOwner()
  val circuit = buildCircuit()

  application {
    Window(
      onCloseRequest = {
        retainedValuesStoreOwner.dispose()
        exitApplication()
      },
    ) {
      RetainedValuesStoreProvider(owner = retainedValuesStoreOwner) {
        CircuitCompositionLocals(circuit) {
          // App content
        }
      }
    }
  }
}
```

The explicit owner is required here because Compose Desktop windows do not provide a `LocalViewModelStoreOwner`. Hosts that do provide one, such as apps using Compose Multiplatform navigation, can omit the `owner` parameter and let the provider scope retention to that `ViewModelStore`.

### iOS Example

Keep the owner outside the view controller factory so retained values survive the SwiftUI integration recreating the `ComposeUIViewController`:

```kotlin
// Deliberately process-scoped. A new ComposeUIViewController gets a new composition,
// so the owner must live outside the factory for retained values to survive.
private val retainedValuesStoreOwner = RetainedValuesStoreOwner()
private val circuit = buildCircuit()

fun MainViewController(): UIViewController {
  CircuitRetainedSettings.useFirstParty = true
  return ComposeUIViewController {
    RetainedValuesStoreProvider(owner = retainedValuesStoreOwner) {
      CircuitCompositionLocals(circuit) {
        // App content
      }
    }
  }
}
```

A process-lifetime owner like this one never needs `dispose()`. Omitting the explicit owner also works on iOS because `ComposeUIViewController` provides a `LocalViewModelStoreOwner`, but that `ViewModelStore` is scoped to the view controller, so retention would end whenever the view controller is recreated.

### Web Example

Keep the owner outside `ComposeViewport` so retained values survive the viewport being disposed and recreated:

```kotlin
fun main() {
  CircuitRetainedSettings.useFirstParty = true
  // Page-scoped. main() returns after setup, but the composition's closures
  // keep these alive for the lifetime of the page.
  val retainedValuesStoreOwner = RetainedValuesStoreOwner()
  val circuit = buildCircuit()

  ComposeViewport {
    RetainedValuesStoreProvider(owner = retainedValuesStoreOwner) {
      CircuitCompositionLocals(circuit) {
        // App content
      }
    }
  }
}
```

A page-lifetime owner never needs `dispose()`. An app that unmounts the Compose canvas and mounts it again should hold the owner wherever that re-mount logic lives, so both mounts share it. As on iOS, omitting the explicit owner works because `ComposeViewport` provides a `LocalViewModelStoreOwner`, but its `ViewModelStore` is cleared when the viewport is disposed, so retention would end with the viewport instead of the page.

For other non-Android hosts, create the owner outside the composition and call `dispose()` when the host shuts down. This is in-memory retention only and does not survive process death. Follow [the upstream issue](https://issuetracker.google.com/issues/467397537) for first-party non-Android support.

If you only use AndroidX `retain` directly, omit the `CircuitRetainedSettings.useFirstParty` line.

### Migration Plan

For new unkeyed, non-saveable state, prefer `retain {}` directly.

Circuit APIs without upstream equivalents remain supported. `retain(key = ...)` preserves explicit-key retention, while `retainSaveable` provides the retained-and-saveable hybrid. The unkeyed `rememberRetained`, saveable `rememberRetained` variants, `rememberRetainedSaveable`, and the `produceRetainedState`/`collectAsRetainedState` conveniences remain supported.

### Retained + saveable

The first-party API has no equivalent to Circuit's retained-and-saveable APIs, where a value is retained across configuration changes and opportunistically saved for process death. Circuit's two-layer mechanism is unaffected by the backing swap and remains available through `retainSaveable`, the saveable `rememberRetained` variants, and `rememberRetainedSaveable`.

### Keyed retention

First-party `retain` is positional-only and does not support explicit keys. Circuit's `retain(key = ...)` preserves explicit-key retention and replaces non-saveable `rememberRetained(key = ...)` calls.

For code moving entirely to first-party APIs with dynamic key spaces, such as a controller per chat ID at one call site, the pattern is a keyed container retained as a single value.

A reference implementation with retention-lifecycle forwarding and composition-refcounted eviction lives in this module's tests as a recipe: [RetainedStoreRecipe.kt](src/jvmTest/kotlin/com/slack/circuit/retained/RetainedStoreRecipe.kt).

```kotlin
val store = retain { RetainedStore<ChatId, ChatController>() }
val controller = store.rememberRetainedEntry(chatId) { ChatController(it) }
```

Despite the similar name, this is unrelated to the per-record `RetainedValuesStore` scoping in `NavigableCircuitContent`. That scoping uses the retain runtime's store registry internally, while `RetainedStore` is a user-facing container for keying your own values within whatever scope you retain it in.
