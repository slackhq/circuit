# [Recipe](index.md): Pick a value from a bottom sheet

**Problem:** the user taps something and a modal bottom sheet of options appears; picking one returns
that value to the caller.

CircuitX's `BottomSheetOverlay` takes a strongly typed model and returns a typed result. `show()` suspends
until the sheet finishes. Requires `circuitx-overlays` and a `ContentWithOverlays` above this UI.

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

If the sheet hosts a reusable component instead of inline content, render that component with
**SubCircuit** and pass the selected value back through its `outerEventSink`. See
[Embed a reusable component](reusable-component-subcircuit.md).

**See also:** [CircuitX overlays](../circuitx/overlays.md) ·
[Ask for confirmation](confirmation-dialog.md) · [Return a result](return-a-result.md)
