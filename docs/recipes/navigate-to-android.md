# [Recipe](index.md): Navigate to an Android Activity or URL

**Problem:** a presenter needs to launch something outside Circuit — another `Activity`, a browser,
the share sheet, a custom tab.

Use `circuitx-android`. Decorate your navigator once with `rememberAndroidScreenAwareNavigator`, then
`goTo` an `AndroidScreen` from a presenter like any other screen.

## Set up the decorated navigator

```kotlin
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val navStack = rememberSaveableNavStack(HomeScreen)
      val navigator = rememberAndroidScreenAwareNavigator(
        rememberCircuitNavigator(navStack),   // the Circuit navigator it wraps
        this@MainActivity,                     // Context used to start activities
      )
      CircuitCompositionLocals(circuit) {
        NavigableCircuitContent(navigator, navStack)
      }
    }
  }
}
```

## Launch an Intent from a presenter

`IntentScreen` is the built-in `AndroidScreen` — wrap any `Intent` and `goTo` it:

```kotlin
return DetailState(detail) { event ->
  when (event) {
    is DetailEvent.OpenInBrowser ->
      navigator.goTo(IntentScreen(Intent(Intent.ACTION_VIEW, event.url.toUri())))
    is DetailEvent.Share ->
      navigator.goTo(IntentScreen(Intent.createChooser(shareIntent(event.text), null)))
  }
}
```

The decorated navigator starts `AndroidScreen`s with Android. Other screens still go to the normal
Circuit back stack.

## Custom Android targets

For non-Intent targets (a custom tab, a third-party SDK launcher), pass your own
`AndroidScreenStarter` instead of a `Context`:

```kotlin
val starter = AndroidScreenStarter { screen ->
  when (screen) {
    is IntentScreen -> { context.startActivity(screen.intent, screen.options); true }
    is CustomTabScreen -> { customTabs.launch(screen.url); true }
    else -> false   // Not handled; let Circuit treat it as a normal screen.
  }
}
val navigator = rememberAndroidScreenAwareNavigator(rememberCircuitNavigator(navStack), starter)
```

!!! tip "Doing more than launching Intents?"
    `rememberAndroidScreenAwareNavigator` is the simple path when Android handoff is your only
    special navigation handling. Once you also gate navigation behind auth, feature flags, or
    destination rewrites, use `AndroidScreenAwareNavigationInterceptor` instead. See
    [Intercept, block, or rewrite navigation](intercept-navigation.md).

**See also:** [CircuitX Android](../circuitx/android.md) · [Navigation](../docs/navigation.md) ·
[Intercept navigation](intercept-navigation.md)
