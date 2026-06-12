# [Recipe](index.md): Retry a failed load

**Problem:** a load failed and the user taps "Retry". You need to re-run the load and show the
loading state again.

Drive retries with a counter held in `rememberRetained`. Use it as a key to `produceRetainedState` —
bumping it re-runs the producer. This keeps the retry trigger in the presenter and avoids exposing
imperative "reload" methods.

```kotlin
@Composable
override fun present(): ArticleState {
  var retryCount by rememberRetained { mutableIntStateOf(0) }

  val result by produceRetainedState<ArticleResult>(ArticleResult.Loading, screen.id, retryCount) {
    value = ArticleResult.Loading
    value = articleRepository.load(screen.id)   // suspend one-shot that returns success/failure
  }

  return when (val current = result) {
    ArticleResult.Loading -> ArticleState.Loading
    is ArticleResult.Success -> ArticleState.Loaded(current.article)
    is ArticleResult.Failure ->
      ArticleState.Error(current.message) { event ->
        when (event) {
          ErrorEvent.Retry -> retryCount++   // changes the key → producer re-runs
        }
      }
  }
}
```

Because `retryCount` is one of the producer's keys, incrementing it cancels any in-flight work,
resets to `Loading`, and re-runs the load.

If your data source is a `Flow` rather than a one-shot suspend call, expose a `refresh()` on the
repository instead and call it from the event — the Flow re-emits and your `produceRetainedState`
collector ([observing a Flow](observe-a-flow.md)) picks it up without a counter.

**See also:** [Show loading, loaded, and error states](loading-states.md) ·
[Pull to refresh](pull-to-refresh.md)
