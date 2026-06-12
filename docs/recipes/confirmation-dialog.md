# [Recipe](index.md): Ask for confirmation with a dialog

**Problem:** before a destructive action (delete, discard), you need a yes/no confirmation and the
answer back in the presenter.

Use an overlay, shown with `OverlayEffect`. The effect suspends on the overlay's result and feeds it
back as an event, so there's no `showDialog: Boolean` flag and no manual coroutine launching.
Requires the `circuit-overlay` artifact and a `ContentWithOverlays` somewhere above this UI.

## One-time setup: `ContentWithOverlays`

Overlays render into an `OverlayHost` exposed by `ContentWithOverlays`. Wrap your navigable content
in it **once**, at the root — every screen below then has `LocalOverlayHost` available, so individual
recipes never set this up again. (If you use shared-element transitions, `SharedElementTransitionLayout`
goes just outside `ContentWithOverlays` — shown here; drop that line if you don't.)

```kotlin
setContent {
  CircuitCompositionLocals(circuit) {
    SharedElementTransitionLayout {            // optional — only if you use shared elements
      ContentWithOverlays {
        NavigableCircuitContent(navigator = navigator, navStack = navStack)
      }
    }
  }
}
```

## Gate the dialog on nullable state, run it with `OverlayEffect`

Model "is the dialog showing?" as a nullable state property — `null` = hidden, non-null carries what
to confirm. `OverlayEffect` runs whenever that property is non-null, `show()`s the overlay, and the
result comes back as an ordinary event. `circuitx-overlays` ships `alertDialogOverlay`, a thin
`Overlay` over Material 3's `AlertDialog`; it resolves to a `DialogResult`
(`Confirm` / `Cancel` / `Dismiss`).

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

This is the "boolean flag → nullable state" pattern: a non-null `pendingDelete` is the single source
of truth for whether the dialog is up, and `OverlayEffect` — not an imperative `LocalOverlayHost.show`
in an `onClick` — is what presents it.

**See also:** [Overlays](../overlays.md) · [CircuitX overlays](../circuitx/overlays.md) ·
[Pick a value from a bottom sheet](bottom-sheet-picker.md)
