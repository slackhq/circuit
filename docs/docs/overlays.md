Overlays
======

The `circuit-overlay` artifact contains an optional API for presenting overlays on top of the current UI.

```kotlin
@Composable
fun SubmitAnswer(state: FormState, modifier: Modifier = Modifier) {
  if (state.promptConfirmation) {
    OverlayEffect {
      // Suspend on the result of the overlay
      val result = show(ConfirmationDialogOverlay(title = "Are you sure?"))
      state.eventSink(SubmitAnswerEvent(result))
    }
  }
}
```

## Usage

The core APIs are the `Overlay` and `OverlayHost` interfaces.

### Overlay

An `Overlay` is composable content that can be shown on top of other content via an `OverlayHost`. Overlays are typically used for one-off request/result flows and should not usually attempt to do any sort of external navigation or make any assumptions about the state of the app. They should only emit a `Result` to the given `OverlayNavigator` parameter when they are done.

```kotlin
interface Overlay<Result : Any> {
  @Composable
  fun Content(navigator: OverlayNavigator<Result>)
}
```

For common overlays, it's useful to create a common `Overlay` subtype that can be reused. For
example: `BottomSheetOverlay`, `ModalOverlay`, `TooltipOverlay`, etc.

### OverlayHost

An `OverlayHost` is provided via composition local and exposes a `suspend show()` function to show an overlay and resume with a typed `Result`.

```kotlin
val result = LocalOverlayHost.current.show(BottomSheetOverlay(...))
```

Where `BottomSheetOverlay` is a custom bottom sheet implementation of an `Overlay`.

In composition, you can also use `OverlayEffect` for a more streamlined API.

```kotlin
OverlayEffect {
  val result = show(BottomSheetOverlay(...))
}
```

## Installation

Add the dependency.

```kotlin
implementation("com.slack.circuit:circuit-overlay:$circuit_version")
```

The simplest starting point for adding overlay support is the `ContentWithOverlays` composable function.

```kotlin
ContentWithOverlays {
  // Your content here
}
```

This will expose a `LocalOverlayHost` composition local that can be used by UIs to show overlays. This also exposes a `LocalOverlayState` composition local that can be used to check the current overlay state (`UNAVAILABLE`, `HIDDEN`, or `SHOWING`).

## `Overlay` vs `PopResult`

Overlays and navigation results can accomplish similar goals, and you should choose the right one for your use case.

|                                            | `Overlay` | `PopResult` |
|--------------------------------------------|-----------|-------------|
| Survives process death                     | ❌         | ✅           |
| Type-safe                                  | ✅         | 🟡          |
| Suspend on result                          | ✅         | ❌           |
| Participates in back stack                 | ❌         | ✅           |
| Supports non-saveable inputs/outputs       | ✅         | ❌           |
| Can participate with the caller's UI       | ✅         | ❌           |
| Can return multiple different result types | ❌         | ✅           |
| Works without a back stack                 | ✅         | ❌           |

*`PopResult` is technically type-safe, but it's not as strongly typed as `Overlay` results since there is nothing inherently requiring the target screen to pop a given result type back.