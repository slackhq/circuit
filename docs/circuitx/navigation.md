# Circuitx Navigation

Circuitx navigation is and optinal intercepting system that lets you hook into and modify
navigation before it happens. The `InterceptingNavigator` sits before a regular Circuit `Navigator`,
giving you a chance to inspect or change navigation events. With the `NavigationInterceptor`
interface, you can handle `goTo`, `pop`, and `resetRoot` calls and decide if they should proceed,
fail, be skipped, or be rewritten to navigate somewhere else. There's also a`NavigationEventListener`
if you just want to know when navigation happens without changing it.
This system is handy for advanced routing, blocking navigation, or tracking navigation events
for analytics.

## Intercepting Overview

Navigation interceptors are useful in several scenarios:

- Rewriting navigation requests (e.g., converting one screen to another)
- Blocking certain navigation paths
- Adding cross-cutting concerns to navigation (e.g., analytics, logging)
- Handling platform-specific navigation (e.g., Android Intents)
- Implementing deep linking or external URL handling

## How It Works

The interception system works through the following process:

1. When a navigation event occurs (like `goTo()`, `pop()`, or `resetRoot()`), it is first passed
   through all registered `NavigationInterceptor`s
2. Each interceptor can choose to:
    - Skip the event (pass it to the next interceptor)
    - Consume the event (preventing further processing)
    - Rewrite the event (changing the destination)
3. If no interceptor consumes the event, it is passed to the underlying `Navigator`
4. Navigation event listeners are then notified of the navigation change

### InterceptingNavigator

`InterceptingNavigator` is an implementation of the `Navigator` interface that applies
interceptors, event listeners, and a intercepting failure notifier to navigation events.

It is created using the `rememberInterceptingNavigator()` composable:

```kotlin
val backStack = rememberSaveableBackStack(Screen)
val navigator = rememberCircuitNavigator(backStack)
val interceptingNavigator = rememberInterceptingNavigator(navigator, interceptors, eventListeners, notifier)
NavigableCircuitContent(navigator = interceptingNavigator, backStack = backStack)
```

### NavigationInterceptor

`NavigationInterceptor` is an interface that defines the contract for intercepting navigation
events in Circuit's navigation system. It has methods that mirror those called on a `Navigator` instance.
Each method returns a result type that indicates how navigation should proceed.

```kotlin
public interface NavigationInterceptor {
  fun goTo(screen: Screen): InterceptedGoToResult
  fun pop(peekBackStack: ImmutableList<Screen>, result: PopResult?): InterceptedPopResult
  fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean): InterceptedResetRootResult
}
```

**Result types**

- `InterceptedResult.Skipped` - The interceptor did not handle the navigation
- `InterceptedResult.Success` - The interceptor handled the navigation successfully
- `InterceptedResult.Failure` - The interceptor encountered an error while handled the navigation
- `InterceptedGoToResult.Rewrite` - The interceptor wants to navigate to a different screen
- `InterceptedResetRootResult.Rewrite` - The interceptor wants to reset to a different root screen

Both `InterceptedResult.Success` and `InterceptedResult.Failure` have a `consumed` flag which
determines whether the navigation event is fully handled by the interceptor.

- If `consumed = true` further processing by subsequent interceptors or the base navigator is prevented
- If `consumed = false`, the navigation event is passed to the next interceptor or the base navigator

When an interceptor returns a `Rewrite` result, the navigation processing restarts with
the *rewritten* screen. All interceptors will operate on this rewritten navigation event as if it
was a new navigation event.

### FailureNotifier

`InterceptingNavigator.FailureNotifier` is a simple interface that provides a mechanism to handle
and report failures that occur during navigation interception in `InterceptingNavigator`.

```kotlin
interface FailureNotifier {
  fun goToFailure(interceptorResult: InterceptedResult.Failure)
  fun popFailure(interceptorResult: InterceptedResult.Failure)
  fun rootResetFailure(interceptorResult: InterceptedResult.Failure)
}
```

### NavigationEventListener

`NavigationEventListener` is an interface that enables monitoring of navigation events within
Circuit's navigation system.

```kotlin
interface NavigationEventListener {
  fun onBackStackChanged(backStack: ImmutableList<Screen>) {}
  fun goTo(screen: Screen) {}
  fun pop(backStack: ImmutableList<Screen>, result: PopResult?) {}
  fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean) {}
}
```

**Event Timing**

The event listeners are only notified if the navigation event was handled by the base navigator and
was not intercepted by a `NavigationInterceptor`.

- `onBackStackChanged`: Called both for the initial state and after any operation that modifies the
  back stack
- `goTo`, `pop`, and `resetRoot`: Called after successful navigation operations that weren't
  intercepted

The failure notifier is called when a `NavigationInterceptor` returns a `Failure` result.
This can be useful for logging, analytics, and debugging navigation issues.

## Example Setup

An example of setting up a `InterceptingNavigator` with interceptors, event listeners, and a
failure notifier.

```kotlin
val interceptors =
  persistentListOf(
    // Order matters! First interceptor is first handle navigation
    AndroidScreenAwareNavigationInterceptor(context),
    AuthInterceptor(authManager),
    UrlRewriteInterceptor,
  )

val eventListeners =
  persistentListOf(
    LoggingNavigationEventListener,
    AnalyticsNavigationEventListener(analytics, crashReporter),
  )

val notifier = AnalyticsFailureNotifier(analytics)
val circuit = buildCircuit()

@Composable
fun App() {
  val backStack = rememberSaveableBackStack(HomeScreen)
  val baseNavigator = rememberCircuitNavigator(backStack)

  // Create the intercepting navigator with all our interceptors
  val navigator =
    rememberInterceptingNavigator(
      navigator = baseNavigator,
      interceptors = interceptors,
      eventListeners = eventListeners,
      notifier = notifier,
    )

  // Use the navigator with your Circuit
  CircuitCompositionLocals(circuit) {
    NavigableCircuitContent(navigator = navigator, backStack = backStack)
  }
}

```

## Recipes

### URL Rewrite Interceptor

This interceptor rewrites a `Screen` to open an external URL:

```kotlin
object UrlRewriteInterceptor : NavigationInterceptor {
  override fun goTo(screen: Screen): InterceptedGoToResult {
    return when (screen) {
      is InfoScreen -> {
        InterceptedGoToResult.Rewrite(
          IntentScreen(Intent(Intent.ACTION_VIEW, "https://example.com/info".toUri()))
        )
      }
      else -> NavigationInterceptor.Skipped
    }
  }
}
```

### Authentication Interceptor

This interceptor synchronously checks if the user is logged in before allowing navigation to
protected screens:

```kotlin
class AuthInterceptor(private val authManager: AuthManager) : NavigationInterceptor {
  override fun goTo(screen: Screen): InterceptedGoToResult {
    // For protected screens, verify authentication
    if (screen is ProtectedScreen && !authManager.isLoggedIn()) {
      // Rewrite to login screen with original destination as a parameter
      return InterceptedGoToResult.Rewrite(LoginScreen(afterLoginDestination = screen))
    }
    return NavigationInterceptor.Skipped
  }
}
```

### Feature Flag Interceptor

This interceptor checks if a feature flag is enabled before allowing navigation to screens.

```kotlin
class FeatureFlagInterceptor(
  private val featureManager: FeatureManager,
  private val screenToFeatureMap: Map<KClass<out Screen>, String>,
) : NavigationInterceptor {

  override fun goTo(screen: Screen): InterceptedResult {
    val requiredFlag = screenToFeatureMap[screen::class] ?: return InterceptedResult.Skipped
    return if (!featureManager.isEnabled(requiredFlag)) {
      InterceptedResult.Failure(
        consumed = true,
        throwable = FeatureNotEnabledException(requiredFlag),
      )
    } else {
      InterceptedResult.Skipped
    }
  }
}

```

### Analytics tracking Event Listener

This event listener combines analytics tracking and crash reporting.

```kotlin
class AnalyticsNavigationEventListener(
  private val analytics: Analytics, // Your analytics system
  private val crashReporter: CrashReporter, // Your crash reporting system
) : NavigationEventListener {

  override fun onBackStackChanged(backStack: ImmutableList<Screen>) {
    crashReporter.tag("backstack", backStack.joinToString { it.analyticsName() ?: "" })
  }

  override fun goTo(screen: Screen) {
    analytics.trackEvent(
      name = "go_to",
      properties = mapOf("screen_name" to screen.analyticsName())
    )
  }

  override fun pop(backStack: ImmutableList<Screen>, result: PopResult?) {
    analytics.trackEvent(
      name = "pop",
      properties = mapOf("screen_name" to backStack.firstOrNull()?.analyticsName()),
    )
  }

  private fun Screen.analyticsName() = this::class.simpleName
}

```

### Debug Event Listener

This event listener logs navigation events to help with development and testing by maintaining
navigation history.

```kotlin
class DebugNavigationEventListener : NavigationEventListener {
  private val navigationHistory = mutableListOf<NavigationEvent>()

  sealed class NavigationEvent {
    data class GoTo(val screen: Screen) : NavigationEvent()
    data class Pop(val poppedScreen: Screen?) : NavigationEvent()
    data class ResetRoot(val newRoot: Screen) : NavigationEvent()
  }

  override fun goTo(screen: Screen) {
    navigationHistory.add(NavigationEvent.GoTo(screen))
  }

  override fun pop(backStack: ImmutableList<Screen>, result: PopResult?) {
    navigationHistory.add(NavigationEvent.Pop(backStack.firstOrNull()))
  }

  override fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean) {
    navigationHistory.add(NavigationEvent.ResetRoot(newRoot))
  }

  fun getNavigationHistory(): List<NavigationEvent> = navigationHistory.toList()

  fun clearHistory() {
    navigationHistory.clear()
  }
}

```

### Analytics Failure Notifier

This failure notifier tracks navigation interceptor failures using analytics.

```kotlin
class AnalyticsFailureNotifier(
  private val analytics: Analytics, // Your analytics system
) : InterceptingNavigator.FailureNotifier {

  override fun goToFailure(interceptorResult: InterceptedResult.Failure) {
    val metadata = mapOf(
      "operation" to "goTo",
      "error" to interceptorResult.reason,
    )
    analytics.trackError("navigation_interceptor_failure", metadata)
  }

  override fun popFailure(interceptorResult: InterceptedResult.Failure) {
    val metadata = mapOf(
      "operation" to "pop",
      "error" to interceptorResult.reason,
    )
    analytics.trackError("navigation_interceptor_failure", metadata)
  }

  override fun rootResetFailure(interceptorResult: InterceptedResult.Failure) {
    val metadata = mapOf(
      "operation" to "resetRoot",
      "error" to interceptorResult.reason,
    )
    analytics.trackError("navigation_interceptor_failure", metadata)
  }
}

```

## Handling `AndroidScreen` with a Navigator or an Interceptor

As an example comparison between Navigator delegation and Navigator interceptors, consider the two
ways to handle Android-specific navigation:

### AndroidScreenAwareNavigator

Using the `rememberAndroidScreenAwareNavigator()` composable:

```kotlin
val backStack = rememberSaveableBackStack(Screen)
val baseNavigator = rememberCircuitNavigator(backStack)
val navigator = rememberAndroidScreenAwareNavigator(baseNavigator, AndroidScreenStarter {})
NavigableCircuitContent(navigator = navigator, backStack = backStack)

```

This creates a new `Navigator` that delegates navigation to the underlying `CircuitNavigator`. It
intercepts navigation events for `AndroidScreen` subtypes, such as `IntentScreen`.

### AndroidScreenAwareNavigationInterceptor

Using the interceptor pattern:

```kotlin
val interceptors =
  persistentListOf(AndroidScreenAwareNavigationInterceptor(AndroidScreenStarter {}))
val backStack = rememberSaveableBackStack(Screen)
val baseNavigator = rememberCircuitNavigator(backStack)
val navigator =
  rememberInterceptingNavigator(navigator = baseNavigator, interceptors = interceptors)
NavigableCircuitContent(navigator = navigator, backStack = backStack)
```

This creates a new `NavigationInterceptor` with a `AndroidScreenAwareNavigationInterceptor`.
It will also intercept and consume navigation events for `AndroidScreen` subtypes, such as
`IntentScreen`.

### Comparison

**Navigator Approach:**

- Simple to use for basic cases
- Directly integrated into the Navigator chain
- Combining other interception behaviors requires more delegated Navigators

**Interceptor Approach:**

- Can be combined with multiple interceptors
- Provides detailed control over success/failure handling
- Can be used with navigation event listeners and failure notifiers

