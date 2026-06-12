# [Recipe](index.md): Debounce a search field

**Problem:** a search box should query as the user types, but not fire a request on every keystroke.

Hold the query as state, expose a `QueryChanged` event, and debounce the query inside a
`produceRetainedState` block using a snapshot `Flow`. The debounce and the search live in the
producer, so they're cancelled and restarted cleanly as the query changes.

```kotlin
@Composable
override fun present(): SearchState {
  var query by rememberRetained { mutableStateOf("") }

  val results by produceRetainedState<List<Hit>>(emptyList()) {
    snapshotFlow { query }
      .debounce(300.milliseconds)
      .distinctUntilChanged()
      .mapLatest { text -> if (text.isBlank()) emptyList() else searchRepository.search(text) }
      .collect { hits -> value = hits }
  }

  return SearchState(query = query, results = results) { event ->
    when (event) {
      is SearchEvent.QueryChanged -> query = event.text
    }
  }
}
```

Key points:

- **`snapshotFlow { query }`** turns the Compose state into a Flow, so `debounce` / `distinctUntilChanged`
  operate on it without manual plumbing.
- **`mapLatest`** cancels an in-flight search when a newer query arrives — you never show stale results.
- The whole pipeline is built **inside** `produceRetainedState`, so it isn't reallocated on
  recomposition (see [observing a Flow](observe-a-flow.md)).

The UI just reports text changes:

```kotlin
TextField(
  value = state.query,
  onValueChange = { text -> state.eventSink(SearchEvent.QueryChanged(text)) },
)
```

**See also:** [Observe a Flow](observe-a-flow.md) · [Show loading states](loading-states.md)
