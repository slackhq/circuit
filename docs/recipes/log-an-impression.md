# [Recipe](index.md): Log an impression once when a screen opens

**Problem:** fire an analytics impression exactly once when a screen (or item) is shown — not on
every recomposition, and not again after a configuration change.

Use CircuitX's impression effects (`circuitx-effects`). They run once for the current retained-state
registry, so they survive recomposition and rotation.

```kotlin
@Composable
override fun present(): ArticleState {
  // Synchronous, fire-once:
  ImpressionEffect { analytics.logArticleView(screen.articleId) }
  // …
}
```

For suspending work (a network beacon, a suspend logging call), use `LaunchedImpressionEffect`. Pass
a key when the impression should run again for a different item:

```kotlin
// Fires once per distinct article id.
LaunchedImpressionEffect(screen.articleId) {
  analytics.logArticleViewAsync(screen.articleId)
}
```

Use a constant key (`LaunchedImpressionEffect(Unit) { … }`) for "once for the lifetime of this
composition, regardless of inputs".

## Re-fire when the user navigates back to the screen

A plain impression effect stays "already fired" when the user pops back to a screen still on the back
stack. If you want to log a fresh impression on re-entry, wrap the navigator with
`rememberImpressionNavigator` and use *that* for navigation:

```kotlin
val navigator = rememberImpressionNavigator(navigator = navigator) {
  analytics.logScreenReentered(screen.articleId)
}
```

| Need                              | Use                             |
|-----------------------------------|---------------------------------|
| sync, once per composition/key    | `ImpressionEffect`              |
| suspend, once per composition/key | `LaunchedImpressionEffect(key)` |
| re-fire when navigated back to    | `rememberImpressionNavigator`   |

**See also:** [CircuitX effects](../circuitx/effects.md)
