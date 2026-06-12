# [Recipe](index.md): Pick a value from a bottom sheet

**Problem:** the user taps something and a modal bottom sheet of options appears; picking one returns
that value to the caller.

CircuitX's `BottomSheetOverlay` takes a strongly-typed input model and returns a typed result, and
`show()` suspends until the sheet finishes. Requires `circuitx-overlays` and a `ContentWithOverlays`
above this UI.

## One-time setup: `ContentWithOverlays`

Overlays render into an `OverlayHost` exposed by `ContentWithOverlays`. Wrap your navigable content
in it **once**, at the root — every screen below then has `LocalOverlayHost` available. (If you use
shared-element transitions, `SharedElementTransitionLayout` goes just outside `ContentWithOverlays` —
shown here; drop that line if you don't.)

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

## Show the sheet

Wrap the sheet in a reusable suspend function on `OverlayHost`:

```kotlin
suspend fun OverlayHost.pickPriority(current: Priority): Priority =
  show(
    BottomSheetOverlay(model = Priority.entries.toList()) { priorities, overlayNavigator ->
      Column {
        priorities.forEach { priority ->
          ListItem(
            headlineContent = { Text(priority.label) },
            trailingContent = { if (priority == current) Icon(Icons.Default.Check, null) },
            modifier = Modifier.clickable { overlayNavigator.finish(priority) },
          )
        }
      }
    }
  )
```

`overlayNavigator.finish(priority)` closes the sheet and resumes `show()` with that value.

Drive it from presenter state so the UI stays declarative — a nullable property says "the picker is
open", and the result comes back as an event:

```kotlin
@Composable
fun TaskEditor(state: TaskState, modifier: Modifier = Modifier) {
  if (state.isPickingPriority) {
    OverlayEffect(state.isPickingPriority) {
      val picked = pickPriority(state.priority)
      state.eventSink(TaskEvent.PriorityPicked(picked))
    }
  }
  // … editor fields, one of which opens the picker via an event …
}
```

If the sheet should host a whole reusable component rather than inline content, render it with
**SubCircuit** inside the sheet body — the component delegates its events (including "dismiss with this
value") up to the host via its `outerEventSink`, which is exactly the embedded-component pattern. See
[Embed a reusable component](reusable-component-subcircuit.md).

**See also:** [CircuitX overlays](../circuitx/overlays.md) ·
[Ask for confirmation](confirmation-dialog.md) · [Return a result](return-a-result.md)
