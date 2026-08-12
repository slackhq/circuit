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

Compose now ships a first-party retain API (`androidx.compose.runtime:runtime-retain`, stable since Compose 1.10) that covers the same core problem as `rememberRetained`. Circuit is migrating toward it in phases.

Circuit uses the first-party backing by default on Android, where Compose UI installs a lifecycle-aware `RetainedValuesStore`. To use the previous ViewModel backing, set the flag before the first composition:

```kotlin
// Set before the first composition, such as in Application.onCreate() or main().
CircuitRetainedSettings.useFirstParty = false
```

Other platforms continue to use their existing Circuit-retained backing by default. On JVM, iOS, macOS, and web, apps that install an appropriate `RetainedValuesStore` can opt in by setting `CircuitRetainedSettings.useFirstParty = true` before the first composition.

With first-party backing enabled, `lifecycleRetainedStateRegistry()` is backed by a single root-level `retain` call instead of a Circuit-managed hidden `ViewModel`. Survival across configuration changes is then driven by the `RetainedValuesStore` installed in the composition. All `rememberRetained`/`rememberRetainedSaveable` semantics are unchanged. The setting remains experimental (`@ExperimentalCircuitRetainedApi`).

With first-party backing enabled, `NavigableCircuitContent` also scopes a `RetainedValuesStore` to each nav record, so first-party `retain {}` calls inside presenters and UIs get per-record lifetimes side by side with `rememberRetained`: values survive while their record is in the nav stack (including across configuration changes) and are retired when the record is popped.

### Migration Plan

The backing swap changes the retention transport and scopes `retain {}` per record, with no API changes. From here, new unkeyed, non-saveable usages can prefer `retain {}` directly.

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
