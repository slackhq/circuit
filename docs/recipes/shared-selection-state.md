# [Recipe](index.md): Share selection state across list items

**Problem:** long-pressing a list row enters Gmail-style bulk-selection. Every row must know
selection mode is active (even though *another* row triggered it) and show a checkbox.

Rows in selection mode are not independent; they share live state. Keep that state in **one parent
presenter** and render rows as plain composables. If per-row logic gets heavy, use a
[StateProducer](../presenter-patterns.md#pattern-3-stateproducer).

```kotlin
data class InboxState(
  val rows: List<RowState>,
  val inSelectionMode: Boolean,
  val eventSink: (InboxEvent) -> Unit,
) : CircuitUiState

data class RowState(val id: MessageId, val subject: String, val selected: Boolean)

sealed interface InboxEvent : CircuitUiEvent {
  data class LongPress(val id: MessageId) : InboxEvent
  data class Toggle(val id: MessageId) : InboxEvent
  data object ClearSelection : InboxEvent
}
```

The parent presenter owns the selection set; every row's `selected` flag is derived from it, so a
change made by one row is reflected in all of them on the next state emission:

```kotlin
@Composable
override fun present(): InboxState {
  val messages by produceRetainedState<List<Message>>(emptyList()) {
    inboxRepository.messages().collect { latest -> value = latest }
  }
  val selected = rememberRetained { mutableStateSetOf<MessageId>() }

  // Rebuild rows only when messages or selection changes.
  val rows by remember(messages) {
    derivedStateOf {
      messages.map { message ->
        RowState(message.id, message.subject, selected = message.id in selected)
      }
    }
  }

  return InboxState(rows = rows, inSelectionMode = selected.isNotEmpty()) { event ->
    when (event) {
      is InboxEvent.LongPress -> selected.add(event.id)
      is InboxEvent.Toggle ->
        if (event.id in selected) selected.remove(event.id) else selected.add(event.id)
      InboxEvent.ClearSelection -> selected.clear()
    }
  }
}
```

Rows read their `selected` flag and report events upward:

```kotlin
@Composable
private fun MessageRow(row: RowState, inSelectionMode: Boolean, eventSink: (InboxEvent) -> Unit) {
  Row(
    Modifier.combinedClickable(
      onClick = { if (inSelectionMode) eventSink(InboxEvent.Toggle(row.id)) /* else open */ },
      onLongClick = { eventSink(InboxEvent.LongPress(row.id)) },
    )
  ) {
    if (inSelectionMode) Checkbox(checked = row.selected, onCheckedChange = null)
    Text(row.subject)
  }
}
```

**Rule of thumb:** if children need shared state, model that state in the parent.

**See also:** [Presenter patterns: StateProducer](../presenter-patterns.md#pattern-3-stateproducer) ·
[Embed a reusable component](reusable-component-subcircuit.md)
