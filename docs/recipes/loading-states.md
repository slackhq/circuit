# [Recipe](index.md): Show loading, loaded, and error states

**Problem:** a screen loads data asynchronously and needs to render distinct loading, success, and
error UI.

Model the three outcomes as a sealed `State`, and give each variant only the events that make sense
for it — `Loading` has no events, `Error` only retries. The presenter maps the repository's result
into the right variant.

```kotlin
sealed interface ProfileState : CircuitUiState {
  data object Loading : ProfileState

  data class Loaded(
    val name: String,
    val eventSink: (LoadedEvent) -> Unit,
  ) : ProfileState

  data class Error(
    val message: String,
    val eventSink: (ErrorEvent) -> Unit,
  ) : ProfileState
}

sealed interface LoadedEvent : CircuitUiEvent {
  data object Refresh : LoadedEvent
}

sealed interface ErrorEvent : CircuitUiEvent {
  data object Retry : ErrorEvent
}
```

The presenter collects the repository (which exposes its own loading/success/error result type) and
translates each case. See [observing a Flow](observe-a-flow.md) for why the Flow is built inside
`produceRetainedState`.

```kotlin
@Composable
override fun present(): ProfileState {
  val result by produceRetainedState<ProfileResult>(ProfileResult.Loading, screen.userId) {
    profileRepository.profile(screen.userId).collect { fetched -> value = fetched }
  }

  return when (val current = result) {
    ProfileResult.Loading -> ProfileState.Loading
    is ProfileResult.Success ->
      ProfileState.Loaded(name = current.name) { event ->
        when (event) {
          LoadedEvent.Refresh -> profileRepository.refresh(screen.userId)
        }
      }
    is ProfileResult.Failure ->
      ProfileState.Error(message = current.message) { event ->
        when (event) {
          ErrorEvent.Retry -> profileRepository.refresh(screen.userId)
        }
      }
  }
}
```

The UI dispatches on the sealed state — each branch only sees the events it's allowed to send:

```kotlin
@Composable
fun Profile(state: ProfileState, modifier: Modifier = Modifier) {
  when (state) {
    ProfileState.Loading -> CircularProgressIndicator(modifier)
    is ProfileState.Loaded ->
      ProfileBody(state.name, onRefresh = { state.eventSink(LoadedEvent.Refresh) }, modifier)
    is ProfileState.Error ->
      ErrorView(state.message, onRetry = { state.eventSink(ErrorEvent.Retry) }, modifier)
  }
}
```

**See also:** [States and Events](../states-and-events.md) ·
[Retry a failed load](retry-a-failed-load.md) · [Pull to refresh](pull-to-refresh.md)
