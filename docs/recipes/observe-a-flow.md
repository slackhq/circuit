# [Recipe](index.md): Observe a Flow or repository without leaking it

**Problem:** your presenter needs to turn a `Flow` (from a repository, database, or use-case) into
Circuit state — without leaking the Flow or rebuilding it on every recomposition.

Use `produceRetainedState` and **build the Flow inside the block**. The result is retained across
recomposition, configuration changes, and the back stack. The Flow is collected only while the block
is active.

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

`collectAsRetainedState` works, but it takes the Flow as a parameter. An inline
`repository.messages()` expression and its operator chain are rebuilt in composition on every
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

`collectAsRetainedState` is fine when the Flow is already a stable reference. `produceRetainedState`
is the safer default for repository calls and operator chains.

## Never `rememberRetained` a raw Flow instance

```kotlin
// Retains the Flow itself, including anything it captures.
val messages by rememberRetained { messageRepository.messages(id) }.collectAsState(emptyList())
```

Retain the *result*, not the Flow. Building a chain inside `remember` is fine because it runs once,
but do not hold the Flow as retained state. The same applies to a `Navigator` or `Context`.

**See also:** [Retention reference](../presenter.md#retention) ·
[Keep UI state across config change](keep-state-across-config-change.md)
