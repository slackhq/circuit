# [Recipe](index.md): Paginate a list (load more on scroll)

**Problem:** a long list loads a page at a time; reaching the bottom should fetch the next page,
without firing duplicate requests or losing accumulated items across recomposition.

Pagination has several moving parts:

- accumulated items
- the next cursor
- an in-flight flag
- an end-of-list flag

Keep them in a small **presentation state holder** (the same idea as `EmailFieldState` in 
[Scaling Presenters](../docs/presenter-patterns.md#use-cases-separating-business-logic)). The presenter creates the holder with `rememberRetained` and drives 
loading from an effect and events.

## The holder

A plain `@Stable` class with private-set state and one suspend `loadNext()`. It's reusable across any
cursor-paged list.

```kotlin
@Stable
class PagingState<T> {
  private val loaded = mutableStateListOf<T>()
  val items: List<T> get() = loaded

  var isLoadingMore by mutableStateOf(false)
    private set
  var endReached by mutableStateOf(false)
    private set

  private var nextCursor: String? = null
  private val mutex = Mutex()

  // Pass the fetcher per call so the retained holder only stores paging data.
  suspend fun loadNext(fetchPage: suspend (cursor: String?) -> Page<T>) {
    // Return early when another load is already running.
    if (endReached || isLoadingMore) return
    mutex.withLock {
      if (endReached) return
      isLoadingMore = true
      try {
        val page = fetchPage(nextCursor)
        loaded.addAll(page.items)
        nextCursor = page.nextCursor
        endReached = page.nextCursor == null
      } finally {
        isLoadingMore = false
      }
    }
  }
}
```

The holder stores only paging data: items, cursor, and flags. Pass the fetcher to `loadNext()` so the
retained holder does not keep the repository on the back stack.

`Mutex.withLock` serializes loads so overlapping `LoadMore` events cannot double-fetch.
`isLoadingMore` is observable state for the UI's loading spinner.

## The presenter

Create the holder with `rememberRetained` so accumulated pages survive rotation and the back stack.
Load the first page from `LaunchedImpressionEffect`; handle `LoadMore` events by launching
`loadNext()`.

```kotlin
@Composable
override fun present(): FeedState {
  val paging = rememberRetained { PagingState<FeedItem>() }
  val scope = rememberCoroutineScope()

  // feedRepository comes from DI. Pass its fetcher into the retained holder.
  LaunchedImpressionEffect(Unit) { paging.loadNext(feedRepository::page) }   // first page on open

  return FeedState(items = paging.items, isLoadingMore = paging.isLoadingMore) { event ->
    when (event) {
      FeedEvent.LoadMore -> scope.launch { paging.loadNext(feedRepository::page) }
    }
  }
}
```

!!! warning "Don't capture the repository in the retained holder"
    `rememberRetained { PagingState<FeedItem>() }` retains **only data** (items, cursor, flags) — never
    the repository. Constructing it as `rememberRetained { PagingState(feedRepository::page) }` would
    capture `feedRepository` (and its scopes, `Context`, etc.) in retained state, keeping it alive
    across rotation and the back stack — the same leak class as
    [retaining a `Flow`](observe-a-flow.md). The repository lives on the presenter via DI; the holder
    receives its fetcher per `loadNext()` call.

## The UI

The UI owns the `LazyListState`, so the "near the end" detection lives here. Use `derivedStateOf` so
the trigger changes only when the threshold is crossed.

```kotlin
@Composable
fun Feed(state: FeedState, modifier: Modifier = Modifier) {
  val listState = rememberLazyListState()
  val shouldLoadMore by remember {
    derivedStateOf {
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      lastVisible >= state.items.lastIndex - PREFETCH_DISTANCE
    }
  }

  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) state.eventSink(FeedEvent.LoadMore)
  }

  LazyColumn(state = listState, modifier = modifier) {
    items(state.items, key = { item -> item.id }) { item -> FeedRow(item) }
    if (state.isLoadingMore) {
      item { CircularProgressIndicator(Modifier.fillMaxWidth().wrapContentWidth()) }
    }
  }
}

private const val PREFETCH_DISTANCE = 5
```

**Heavier paging?** If you need placeholders, retries, and refresh out of the box,
[Jetpack Paging](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)'s
`Pager` exposes a `Flow<PagingData<T>>` you can collect with
[`produceRetainedState`](observe-a-flow.md) instead of hand-rolling the holder above.

**See also:** [Observe a Flow](observe-a-flow.md) ·
[Scaling Presenters: state holders](../docs/presenter-patterns.md#use-cases-separating-business-logic) ·
[Keep UI state across config change](keep-state-across-config-change.md)
