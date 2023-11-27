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
  implementation("com.slack.circuit:circuitx-android:<version>")
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

## Effects

CircuitX provides some effects for use with logging/analytics. These effects are typically used in 
circuit presenters for tracking `impressions` and will run only once until forgotten based on the 
current circuit retained strategy.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-effects:<version>")
}
```

### ImpressionEffect

`ImpressionEffect` is a simple single fire side effect useful for logging or analytics. 
This `impression` will run only once until it is forgotten based on the current `RetainedStateRegistry`.

```kotlin
ImpressionEffect {
  // Impression 
}
```

### LaunchedImpressionEffect

This is useful for async single fire side effects like logging or analytics. This effect will run a 
suspendable `impression` once until it is forgotten based on the `RetainedStateRegistry`.

```kotlin
LaunchedImpressionEffect {
  // Impression 
}
```

### RememberImpressionNavigator

A `LaunchedImpressionEffect` that is useful for async single fire side effects like logging or
analytics that need to be navigation aware. This will run the `impression` again if it re-enters
the composition after a navigation event.

```kotlin
val navigator = rememberImpressionNavigator(
  navigator = Navigator()
) {
  // Impression
}
```

## Gesture Navigation

CircuitX provides `NavDecoration` implementation which support navigation through appropriate
gestures on certain platforms.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-gesture-navigation:<version>")
}
```

To enable gesture navigation support, you can use the use the `GestureNavigationDecoration`function:

```kotlin
NavigableCircuitContent(
  navigator = navigator,
  backstack = backstack,
  decoration = GestureNavigationDecoration(
    // Pop the back stack once the user has gone 'back'
    navigator::pop
  )
)
```

### Android

On Android, this supports the [Predictive back gesture](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture) which is available on Android 14 and later (API level 34+). On older platforms, Circuit's default
`NavDecoration` decoration is used instead.

<figure>
  <video controls width="300" loop=true>
    <source src="../images/gesturenav_android.mp4" type="video/mp4" />
  </video>
  <figcaption><a href="https://github.com/slackhq/circuit/tree/main/samples/star">Star sample</a> running on an Android 14 device</figcaption>
</figure>

### iOS

On iOS, this simulates iOS's 'Interactive Pop Gesture' in Compose UI, allowing the user to swipe Circuit UIs away. As this is
a simulation of the native behavior, it does not match the native functionality perfectly. However, it is a good approximation.

<figure>
  <video controls width="300" loop=true>
    <source src="../images/gesturenav_ios.mp4" type="video/mp4" />
  </video>
  <figcaption><a href="https://github.com/chrisbanes/tivi">Tivi</a> app running on iPhone</figcaption>
</figure>

### Other platforms

On other platforms we defer to Circuit's default `NavDecoration` decoration.

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
