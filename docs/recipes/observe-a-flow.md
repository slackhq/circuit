# [Recipe](index.md): Observe a Flow or repository without leaking it

**Problem:** your presenter needs to turn a `Flow` (from a repository, database, or use-case) into
Circuit state — without leaking the Flow or rebuilding it on every recomposition.

Use `produceRetainedState` and **build the Flow inside the block**. The result is retained across
recomposition, configuration changes, and the back stack; the Flow itself is scoped to the block and
torn down correctly.

```kotlin
@Composable
override fun present(): ChannelState {
  val messages by produceRetainedState<List<Message>>(emptyList(), screen.channelId) {
    messageRepository.messages(screen.channelId).collect { latest -> value = latest }
  }
  return ChannelState(messages)
}
```

Pass any keys that should restart collection (here, `screen.channelId`) as arguments — the block
re-runs when a key changes.

## Don't pass the Flow as an argument

`collectAsRetainedState` works, but it takes the Flow as a parameter — so an inline
`repository.messages()` expression (and its whole operator chain) gets rebuilt in composition on every
recomposition:

```kotlin
// ⚠️ Rebuilds the chain in composition every recomposition.
val messages by messageRepository.messages(screen.channelId)
  .map { messages -> messages.sortedByDescending(Message::timestamp) }
  .collectAsRetainedState(initial = emptyList())

// ✅ Built once, inside the block, keyed on channelId.
val messages by produceRetainedState<List<Message>>(emptyList(), screen.channelId) {
  messageRepository.messages(screen.channelId)
    .map { messages -> messages.sortedByDescending(Message::timestamp) }
    .collect { sorted -> value = sorted }
}
```

`collectAsRetainedState` is fine when the Flow is already a stable reference; `produceRetainedState`
is the safer default because it makes it hard to allocate the chain in composition by accident.

## Never `rememberRetained` a raw Flow instance

```kotlin
// 🚫 Retains the Flow itself — keeps everything it captures (repos, scopes) alive on the back stack.
val messages by rememberRetained { messageRepository.messages(id) }.collectAsState(emptyList())
```

Retain the *result*, not the Flow. (Building a full chain inside a `remember` block is fine — it runs
once — but don't hold the Flow as retained state, and never `rememberRetained` a `Navigator` or
`Context`.)

**See also:** [Retention reference](../presenter.md#retention) ·
[Keep UI state across config change](keep-state-across-config-change.md)
