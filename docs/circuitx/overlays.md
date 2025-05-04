CircuitX provides a few out-of-the-box `Overlay` implementations that you can use to build common
UIs.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-overlays:<version>")
}
```

### `BottomSheetOverlay`

`BottomSheetOverlay` is an overlay that shows a bottom sheet with a strongly-typed API for the input
model to the sheet content and result type. This allows you to easily use a bottom sheet to prompt
for user input and suspend the underlying Circuit content until that result is returned.

```kotlin
/** A hypothetical bottom sheet of available actions when long-pressing a list item. */
suspend fun OverlayHost.showLongPressActionsSheet(): Action {
  return show(
    BottomSheetOverlay(
      model = listOfActions()
    ) { actions, overlayNavigator ->
      ActionsSheet(
        actions,
        overlayNavigator::finish // Finish the overlay with the clicked Action
      )
    }
  )
}

@Composable
fun ActionsSheet(actions: List<Action>, onActionClicked: (Action) -> Unit) {
  Column {
    actions.forEach { action ->
      TextButton(onClick = { onActionClicked(action) }) {
        Text(action.title)
      }
    }
  }
}
```

### Dialog Overlays

`alertDialogOverlay` is function that returns an Overlay that shows a simple confirmation dialog with configurable inputs. This acts more or less as an `Overlay` shim over the Material 3 `AlertDialog` API.

```kotlin
/** A hypothetical confirmation dialog. */
suspend fun OverlayHost.showConfirmationDialog(): Action {
  return show(
    alertDialogOverlay(
      titleText = { Text("Are you sure?") },
      confirmButton = { onClick -> Button(onClick = onClick) { Text("Yes") } },
      dismissButton = { onClick -> Button(onClick = onClick) { Text("No") } },
    )
  )
}
```

There are also more generic `BasicAlertDialog` and `BasicDialog` implementations that allow more customization.

### `FullScreenOverlay`

Sometimes it's useful to have a full-screen overlay that can be used to show a screen in full above
the current content. This API is fairly simple to use and just takes a `Screen` input of what
content you want to show in the overlay.

```kotlin
overlayHost.showFullScreenOverlay(
  ImageViewerScreen(id = url, url = url, placeholderKey = name)
)
```

!!! info "When to use `FullScreenOverlay` vs navigating to a `Screen`?"
While they achieve similar results, the key difference is that `FullScreenOverlay` is
inherently an ephemeral UI that is _controlled_ by an underlying primary UI. It cannot
navigate elsewhere and it does not participate in the backstack.
