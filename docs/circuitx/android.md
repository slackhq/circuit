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
      val backStack = rememberSaveableBackStack(root = HomeScreen)
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