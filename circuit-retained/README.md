# Module circuit-retained

This optional artifact contains a alternative implementations of `rememberRetained`, `produceRetainedState`, `collectAsRetainedState()`, etc. This is
useful for cases where you want to retain non-saveable state across configuration changes or across a back stack. This
comes at the cost of not participating in the `SavedStateRegistry` and thus not being able to persist across process death, but added flexibility of not
requiring Saveable values.

## Installation

This is automatically set up and available when you use it on available platforms when you use `CircuitCompositionLocals {}`.

If using `NavigableCircuitContent`, a `RetainedStateRegistry` is set up for each back stack record automatically as well.

### Advanced usage

By default, `LocalRetainedStateRegistry` composition local will use a no-op instance. You can provide custom implementations of this to tie into whatever
lifecycle is relevant for your app (back stack, hierarchical, etc). The platform default implementations are accessibly via `continuityRetainedStateRegistry()`.

```kotlin
CompositionLocalProvider(
  LocalRetainedStateRegistry provides continuityRetainedStateRegistry(),
) {
  // Content
}
```
