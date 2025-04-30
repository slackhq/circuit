CircuitX provides some effects for use with logging/analytics. These effects are typically used in
Circuit presenters for tracking `impressions` and will run only once until forgotten based on the
current circuit-retained strategy.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-effects:<version>")
}
```

### ImpressionEffect

`ImpressionEffect` is a simple single fire side effect useful for logging or analytics.
This `impression` will run only once until it is forgotten based on the current `RetainedStateRegistry`.

```kotlin
ImpressionEffect {
  // Impression 
}
```

### LaunchedImpressionEffect

This is useful for async single fire side effects like logging or analytics. This effect will run a
suspendable `impression` once until it is forgotten based on the `RetainedStateRegistry`.

```kotlin
LaunchedImpressionEffect {
  // Impression 
}
```

### RememberImpressionNavigator

A `LaunchedImpressionEffect` that is useful for async single fire side effects like logging or
analytics that need to be navigation aware. This will run the `impression` again if it re-enters
the composition after a navigation event.

```kotlin
val navigator = rememberImpressionNavigator(
  navigator = Navigator()
) {
  // Impression
}
```
