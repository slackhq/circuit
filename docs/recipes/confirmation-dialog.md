# [Recipe](index.md): Ask for confirmation with a dialog

**Problem:** before a destructive action (delete, discard), you need a yes/no confirmation and the
answer back in the presenter.

Use an overlay with `OverlayEffect`. The effect suspends on the result and sends it back as an event.
Requires the `circuit-overlay` artifact and a `ContentWithOverlays` above this UI.

## One-time setup: `ContentWithOverlays`

Wrap your navigable content in `ContentWithOverlays` once at the composition root. Every screen within it can
show overlays. If you use shared-element transitions, put `SharedElementTransitionLayout` just
outside `ContentWithOverlays`; otherwise omit it.

```kotlin
setContent {
  CircuitCompositionLocals(circuit) {
    SharedElementTransitionLayout {            // Optional: only if you use shared elements.
      ContentWithOverlays {
        NavigableCircuitContent(navigator = navigator, navStack = navStack)
      }
    }
  }
}
```

## Show the dialog from nullable state

Use a nullable state property for the pending confirmation. `null` means hidden; a non-null value
carries what to confirm. `OverlayEffect` shows the overlay when that value is present, and
`alertDialogOverlay` returns a `DialogResult` (`Confirm`, `Cancel`, or `Dismiss`).

```kotlin
// State carries what to confirm, or null.
data class ItemState(
  val pendingDelete: ItemId?,
  val eventSink: (ItemEvent) -> Unit,
) : CircuitUiState
```

```kotlin
@Composable
fun Item(state: ItemState, modifier: Modifier = Modifier) {
  if (state.pendingDelete != null) {
    OverlayEffect(state.pendingDelete) {
      val result = show(
        alertDialogOverlay(
          title = { Text("Delete this item?") },
          confirmButton = { onClick -> Button(onClick = onClick) { Text("Delete") } },
          dismissButton = { onClick -> TextButton(onClick = onClick) { Text("Cancel") } },
        )
      )
      val confirmed = result == DialogResult.Confirm
      state.eventSink(ItemEvent.DeleteAnswered(state.pendingDelete, confirmed))
    }
  }
  // … the rest of the item …
}
```

A button in the UI requests the dialog by sending an event; the presenter sets `pendingDelete`, which
makes `OverlayEffect` fire:

```kotlin
IconButton(onClick = { state.eventSink(ItemEvent.DeleteClicked(item.id)) }) {
  Icon(Icons.Default.Delete, contentDescription = "Delete")
}
```

Keep the pending value in state. The presenter sets `pendingDelete`, the UI shows the dialog, and the
answer returns through `DeleteAnswered`.

**See also:** [Overlays](../docs/overlays.md) · [CircuitX overlays](../circuitx/overlays.md) ·
[Pick a value from a bottom sheet](bottom-sheet-picker.md)
