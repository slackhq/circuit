# [Recipe](index.md): Intercept, block, or rewrite navigation

**Problem:** before a `goTo` / `pop` / `resetRoot` actually happens, you want to inspect it and
either let it through, block it, send it somewhere else, or hand it off to something outside Circuit
(an Activity, an external URL).

Use `circuitx-navigation`. A `NavigationInterceptor` runs before the real `Navigator` and can
**skip**, **consume**, **fail**, or **rewrite** a navigation event. Wire interceptors up with
`rememberInterceptingNavigator`.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-navigation:<version>")
}
```

## Wire it up

`rememberInterceptingNavigator` wraps your base navigator. Everything else (`NavigableCircuitContent`)
takes the intercepting navigator in place of the original.

```kotlin
@Composable
fun App(circuit: Circuit) {
  val navStack = rememberSaveableNavStack(HomeScreen)
  val baseNavigator = rememberCircuitNavigator(navStack) {
    // Do something when the root screen is popped, usually exiting the app
  }

  val navigator = rememberInterceptingNavigator(
    navigator = baseNavigator,
    interceptors = listOf(AuthInterceptor(authManager), UrlRewriteInterceptor),
  )

  CircuitCompositionLocals(circuit) {
    NavigableCircuitContent(navigator = navigator, navStack = navStack)
  }
}
```

Interceptors run **in order**. The first one to consume or rewrite the event wins.

## The four outcomes

Each `NavigationInterceptor` method returns an `InterceptedResult`. All methods default to `Skipped`,
so you only override the ones you care about (usually `goTo`).

| Result                                        | Effect                                                |
|-----------------------------------------------|-------------------------------------------------------|
| `InterceptedResult.Skipped`                   | pass to the next interceptor, then the real navigator |
| `InterceptedResult.Success(consumed = true)`  | handled it; stop here                                 |
| `InterceptedResult.Failure(consumed, reason)` | failed; `consumed` decides whether to stop            |
| `InterceptedResult.Rewrite(newScreen)`        | navigate somewhere else instead                       |

`NavigationInterceptor.Skipped` and `NavigationInterceptor.SuccessConsumed` are shorthands for the
two most common ones.

## Block navigation (consume it)

Stop navigation to a screen the user isn't allowed to reach — e.g. a feature behind a flag. Returning
a consumed `Failure` halts it; the optional `reason` flows to a `FailureNotifier` for logging.

```kotlin
class FeatureFlagInterceptor(private val features: FeatureManager) : NavigationInterceptor {
  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    val flag = (screen as? Flagged)?.requiredFlag ?: return InterceptedResult.Skipped
    return if (features.isEnabled(flag)) {
      InterceptedResult.Skipped                          // allow it through
    } else {
      InterceptedResult.Failure(consumed = true, reason = DisabledFeature(flag))
    }
  }
}
```

## Rewrite navigation (send it elsewhere)

Redirect one screen to another. The classic case: gate a protected screen behind login, carrying the
original destination so you can resume after auth.

```kotlin
class AuthInterceptor(private val auth: AuthManager) : NavigationInterceptor {
  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    return if (screen is ProtectedScreen && !auth.isLoggedIn) {
      InterceptedResult.Rewrite(LoginScreen(returnTo = screen))   // navigate here instead
    } else {
      InterceptedResult.Skipped
    }
  }
}
```

A `Rewrite` restarts interception with the new event, so every interceptor gets a shot at the
rewritten screen too.

## Hand off to Android (Activities, URLs)

CircuitX ships `AndroidScreenAwareNavigationInterceptor`. Add it to the list and any
`AndroidScreen`, such as `IntentScreen`, is started instead of pushed onto the back stack:

```kotlin
val navigator = rememberInterceptingNavigator(
  navigator = baseNavigator,
  interceptors = listOf(
    AndroidScreenAwareNavigationInterceptor(context),   // consumes AndroidScreens
    AuthInterceptor(authManager),
  ),
)
```

### Interceptor vs. `rememberAndroidScreenAwareNavigator`

Both route `AndroidScreen`s out to Android. Pick based on what else your navigator needs to do:

- **[`rememberAndroidScreenAwareNavigator`](navigate-to-android.md)** — a navigator decorator. Simplest
  when launching Intents is the *only* special handling you need.
- **`AndroidScreenAwareNavigationInterceptor`** — the same behavior as one interceptor in a list, so
  it composes with auth/feature-flag/rewrite/analytics interceptors and gains failure-notifier
  support. Reach for it once you have more than one cross-cutting navigation concern.

## Observe without changing — event listeners

If you only want to observe navigation for analytics or logging, pass `eventListeners` instead of
interceptors. They are notified after navigation that interceptors did not consume.

```kotlin
rememberInterceptingNavigator(
  navigator = baseNavigator,
  interceptors = interceptors,
  eventListeners = listOf(AnalyticsNavigationEventListener(analytics)),
  notifier = AnalyticsFailureNotifier(analytics),   // called on Failure results
)
```

**See also:** [CircuitX navigation](../circuitx/navigation.md) (full interface + listener/notifier
reference) · [Navigate to an Android Activity or URL](navigate-to-android.md)
