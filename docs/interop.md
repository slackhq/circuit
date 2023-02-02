Interop
=======

Circuit can interop anywhere that Compose can interop. This includes common cases like Android 
`Views`, RxJava, Kotlin `Flow`, and more.

## `Presenter`

Lean on first-party interop-APIs where possible! See examples of interop with different libraries in the `:samples:interop` project.

## `UI`

### `Ui` -> `View`

Just embed the Circuit in a `ComposeView` like any other Compose UI.

### `View` -> `Ui`

You can wrap your view in an `AndroidView` in a custom `Ui` implementation. 

```kotlin
class ExistingCustomViewUi : Ui<State> {
  @Composable
  fun Content(state: State, modifier: Modifier = Modifier) {
    AndroidView(
      modifier = ...
      factory = { context ->
        ExistingCustomView(context)
      },
      update = { view ->
        view.setState(state)
        view.setOnClickListener { state.eventSink(Event.Click) }
      }
  }
}
```