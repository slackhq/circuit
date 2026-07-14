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

The current, opt-in phase runs Circuit's retention on top of the first-party store with no API or behavior changes:

```kotlin
// Set before the first composition, such as in Application.onCreate() or main().
CircuitRetainedSettings.useFirstParty = true
```

With the flag enabled, `lifecycleRetainedStateRegistry()` is backed by a single root-level `retain` call instead of a Circuit-managed `ViewModel`. Survival across configuration changes is then driven by the `RetainedValuesStore` installed in the composition, such as the lifecycle-aware store Compose UI installs on Android. All `rememberRetained`/`rememberRetainedSaveable` semantics are unchanged.

The flag is experimental (`@ExperimentalCircuitRetainedApi`) and currently affects the targets that have the ViewModel-backed registry (Android, JVM, iOS, macOS, web).

With the flag enabled, `NavigableCircuitContent` also scopes a `RetainedValuesStore` to each nav record, so first-party `retain {}` calls inside presenters and UIs get per-record lifetimes side by side with `rememberRetained`: values survive while their record is in the nav stack (including across configuration changes) and are retired when the record is popped.

### Migration Plan

The opt-in flag above swaps the retention transport and scopes `retain {}` per record, with no API changes. From here, new unkeyed, non-saveable usages can prefer `retain {}` directly.

Once the backing has soaked, APIs with direct first-party equivalents will be deprecated with replacements, like plain `rememberRetained {}` → `retain {}` and `rememberRetainedStateRegistry` → `retainManagedRetainedValuesStore`.

APIs with no upstream equivalent will likely stay or have recipes: (`rememberRetainedSaveable`, `rememberRetained(key = ...)`, the `produceRetainedState`/`collectAsRetainedState` conveniences).

### Retained + saveable

The first-party API has no equivalent to `rememberRetainedSaveable`, where a value is retained across configuration changes and opportunistically saved
for process death. Circuit's two-layer mechanism is unaffected by the backing swap and keeps working under the flag, so `rememberRetainedSaveable`
remains the supported answer for hybrid lifetimes until an upstream equivalent exists (if ever).

### Keyed retention with first-party `retain`

First-party `retain` is positional-only and does not support explicit keys. For dynamic key spaces (for example — a controller per chat ID at a single call site), the pattern is a keyed container retained as a single value.

A reference implementation with retention-lifecycle forwarding and composition-refcounted eviction lives in this module's tests as a recipe: [RetainedStoreRecipe.kt](src/jvmTest/kotlin/com/slack/circuit/retained/RetainedStoreRecipe.kt).

```kotlin
val store = retain { RetainedStore<ChatId, ChatController>() }
val controller = store.rememberRetainedEntry(chatId) { ChatController(it) }
```

Despite the similar name, this is unrelated to the per-record `RetainedValuesStore` scoping in `NavigableCircuitContent`. That is an internal implementation of the retain runtime's store interface for scoping, while `RetainedStore` is a user-facing container for keying your own values within whatever scope you retain it in.
