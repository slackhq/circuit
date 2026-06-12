# [Recipe](index.md): Pull to refresh

**Problem:** the user pulls down to refresh a list. You need a refreshing flag that's separate from
the initial load, plus a way to trigger the refresh.

Keep `isRefreshing` as presenter state, set it when the refresh starts, and clear it when the data
comes back. Trigger the actual work through the repository.

```kotlin
@Composable
override fun present(): FeedState {
  val scope = rememberCoroutineScope()
  var isRefreshing by rememberRetained { mutableStateOf(false) }

  val items by produceRetainedState<List<FeedItem>>(emptyList()) {
    feedRepository.items().collect { latest ->
      value = latest
      isRefreshing = false   // data arrived → stop the spinner
    }
  }

  return FeedState(items = items, isRefreshing = isRefreshing) { event ->
    when (event) {
      FeedEvent.Refresh -> {
        isRefreshing = true
        scope.launch { feedRepository.refresh() }   // short, UI-tied work — see note below
      }
    }
  }
}
```

In the UI, wire `isRefreshing` and the event to Compose's
[`PullToRefreshBox`](https://developer.android.com/develop/ui/compose/components/pull-to-refresh):

```kotlin
@Composable
fun Feed(state: FeedState, modifier: Modifier = Modifier) {
  PullToRefreshBox(
    isRefreshing = state.isRefreshing,
    onRefresh = { state.eventSink(FeedEvent.Refresh) },
    modifier = modifier,
  ) {
    LazyColumn {
      items(state.items, key = { item -> item.id }) { item -> FeedRow(item) }
    }
  }
}
```

!!! note
    `feedRepository.refresh()` should be a quick trigger that causes the underlying Flow to re-emit.
    If it kicks off genuinely long-running work, that work belongs in the data layer scoped to
    something that outlives the screen — not launched from `rememberCoroutineScope()`, which is
    cancelled when the presenter leaves composition. See
    [run a one-shot suspend action](run-suspend-from-event.md).

**See also:** [Retry a failed load](retry-a-failed-load.md) · [Observe a Flow](observe-a-flow.md) ·
[Compose pull-to-refresh](https://developer.android.com/develop/ui/compose/components/pull-to-refresh)
