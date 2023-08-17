CircuitX
========

CircuitX is a suite of extension artifacts for Circuit. These artifacts are intended to be
batteries-included implementations of common use cases, such as out-of-the-box `Overlay` types or
Android navigation interop.

These packages differ from Circuit's core artifacts in a few ways:

- Their APIs may change more frequently during Circuit's development.
- These artifacts won't ship with their own baseline profiles.
- These artifacts are under the `com.slack.circuitx` package prefix.
- These artifacts may be platform-specific where appropriate.

## Android

The `circuitx-android` artifact contains Android-specific extensions for Circuit.

```kotlin
dependencies {
  implementation("com.slack.circuitx:circuitx-android:<version>")
}
```

### Navigation

It can be important for Circuit to be able to navigate to Android targets, such as other activities
or custom tabs. To support this, decorate your existing `Navigator` instance
with `rememberAndroidScreenAwareNavigator()`.

```kotlin
class MainActivity : Activity {
  override fun onCreate(savedInstanceState: Bundle?) {
    setContent {
      val backstack = rememberSaveableBackStack { push(HomeScreen) }
      val navigator = rememberAndroidScreenAwareNavigator(
        rememberCircuitNavigator(backstack), // Decorated navigator
        this@MainActivity
      )
      CircuitCompositionLocals(circuit) {
        NavigableCircuitContent(navigator, backstack)
      }
    }
  }
}
```

`rememberAndroidScreenAwareNavigator()` has two overloads - one that accepts a `Context` and one
that accepts an `AndroidScreenStarter`. The former is just a shorthand for the latter that only
supports `IntentScreen`. You can also implement your own starter that supports other screen types.

`AndroidScreen` is the base `Screen` type that this navigator and `AndroidScreenStarter` interact
with. There is a built-in `IntentScreen` implementation that wraps an `Intent` and an
options `Bundle` to pass to `startActivity()`. Custom `AndroidScreens` can be implemented separately
and route through here, but you should be sure to implement your own `AndroidScreenStarter` to
handle them accordingly.

## Overlays

CircuitX provides a few out-of-the-box `Overlay` implementations that you can use to build common
UIs.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-overlays:<version>")
}
```

### BottomSheetOverlay

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

### FullScreenOverlay

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
