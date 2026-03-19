# NavStack

Circuit `0.33.0` introduces a new navigation architecture with **bidirectional navigation** support, enabling browser-style forward/backward capabilities. This guide explains the changes, when to use each navigation type, and how to migrate existing code.

## Overview

- **NavStack** enables browser-style forward/backward navigation
- New APIs provide richer navigation state information via `NavStackList`
- Existing `BackStack` code works without changes
- All existing navigation patterns continue to work

### Breaking Changes

- `NavDecoration.DecoratedContent` now receives a `NavStackList<NavArgument>` instead of `List<NavArgument>`
- `AnimatedNavEvent` now has new forward & backward transition types
- `circuitx-navigation` interceptor return types changed from specific types (`InterceptedGoToResult`, `InterceptedPopResult`, etc.) to unified `InterceptedResult`
- Rewrite interceptors now use `InterceptedResult.Rewrite(NavEvent)` instead of type-specific rewrites

## Changes

### Navigator

**New Methods:**

```kotlin title="Navigator.kt" hl_lines="8-13"
interface Navigator {
  // Existing methods
  fun goTo(screen: Screen): Boolean
  fun pop(result: PopResult? = null): Screen?
  fun peek(): Screen?
  fun resetRoot(newRoot: Screen, options: StateOptions): List<Screen>

  // NEW: Bidirectional navigation
  fun forward(): Boolean
  fun backward(): Boolean

  // NEW: Access full navigation state
  fun peekNavStack(): NavStackList<Screen>?

  // Discouraged but still works for backwards compatibility  
  fun peekBackStack(): ImmutableList<Screen>
}
```

**Usage Example:**

```kotlin
val navigator = rememberCircuitNavigator(navStack, onRootPop)

// Traditional navigation (truncates forward history)
navigator.goTo(DetailScreen(id = 1))

// Bidirectional navigation (preserves history)
navigator.backward()  // Move back without removing
navigator.forward()   // Move forward to previously visited screen

// Access full navigation state
val navStackList = navigator.peekNavStack()
val forwardScreens = navStackList?.forwardItems
val backwardScreens = navStackList?.backwardItems
```

### NavigableCircuitContent

`NavigableCircuitContent` is now aware of the full navigation stack and provides `NavStackList` to decorations, enabling rendering of forward stack records.

### Artifact `circuit-runtime-navigation`

This artifact will be transitively resolved by the existing artifacts and contains:

- `NavStack` - Core navigation stack interface supporting push/pop and forward/backward traversal
- `NavStackList` - Immutable snapshot of navigation state

```kotlin
dependencies {
  implementation("com.slack.circuit:circuit-runtime-navigation:x.y.z")
}
```

## BackStack vs NavStack

| Feature             | BackStack | NavStack |
|---------------------|-----------|----------|
| Push/Pop            | ✅         | ✅        |
| Backward History    | ✅         | ✅        |
| Forward History     | ❌         | ✅        |
| Forward Navigation  | ❌         | ✅        |
| Backward Navigation | ❌         | ✅        |
| Immutable Snapshots | ❌         | ✅        |

=== "BackStack"
    ```kotlin title="BackStack.kt"
    interface BackStack<R : Record> {
      val size: Int // Size of the stack from root to top
      val topRecord: R? // Always the newest record
      val rootRecord: R?

      fun push(screen: Screen): Boolean
      fun pop(): R?
    }
    ```

=== "NavStack (New)"
    ```kotlin title="NavStack.kt"
    interface NavStack<R : Record> {
      val size: Int // The total number of records in the stack
      val topRecord: R? // The newest record (top of history)
      val currentRecord: R? // The currently active record (may differ from top)
      val rootRecord: R? // The oldest record

      fun push(screen: Screen): Boolean
      fun pop(): R?
    
      // NEW: Bidirectional navigation
      fun forward(): Boolean   // Move toward topRecord
      fun backward(): Boolean  // Move toward rootRecord
    
      // NEW: Immutable snapshot
      fun snapshot(): NavStackList<R>?
    }
    ```

## Migration Guide

### 1: No migration required

**When:**

- Your app works fine with traditional push/pop navigation
- You want to continue using `BackStack` and `rememberSaveableBackStack()`

**Steps:**

1. No changes required, continue using `BackStack`

```kotlin
val backStack = rememberSaveableBackStack(root = HomeScreen)
val navigator = rememberCircuitNavigator(backStack, onRootPop)
```

### 2: Migrate to `NavStack`

**When:**

- You want bidirectional navigation (forward/backward)

**Steps:**

1. Switch to `rememberSaveableNavStack()` from `circuit-foundation`
2. Use the new `Navigator` methods: `forward()`, `backward()`

=== "Before (backstack module)"
    ```kotlin
    import com.slack.circuit.backstack.rememberSaveableBackStack
    //..
    val backStack = rememberSaveableBackStack(root = HomeScreen)
    val navigator = rememberCircuitNavigator(backStack)
    ```

=== "After (circuit-foundation module)"
    ```kotlin
    import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
    //..
    val navStack = rememberSaveableNavStack(root = HomeScreen)
    val navigator = rememberCircuitNavigator(navStack)
    ```

### 3: Custom `NavDecoration` or `AnimatedNavDecorator`

**When:**

If you've implemented a custom decoration/decoratpr, you'll need to update for the `NavStackList` api changes.

!!! warning "NavStackList"
    When migrating to the new `NavStackList` API, ensure you're comparing to the `active` record and not the `top` record or `size` to determine the current screen.

**`NavDecoration` required changes**

=== "Before"
    `DecoratedContent` received a list of the back stack items
    ```kotlin hl_lines="3"
    @Composable
    fun <T : NavArgument> NavDecoration.DecoratedContent(
      args: List<T>,
      content: @Composable (T) -> Unit
    )
    ```

=== "After"
    `DecoratedContent` now receives the full `NavStackList`
    ```kotlin hl_lines="3"
    @Composable
    fun <T: NavArgument> NavDecoration.DecoratedContent(
      args: NavStackList<T>,
      content: @Composable (T) -> Unit
    )
    ```

**`AnimatedNavDecorator` required changes**

Here's an example of changes performed to the `PredictiveBackNavigationDecorator` in the `circuitx-gesture-navigation` artifact:

=== "Before"
    ```kotlin
      override fun updateTransition(args: List<T>): Transition<GestureNavTransitionHolder<T>> {
        val current = remember(args) { targetState(args) }
        val previous =
          remember(args) {
            if (args.size > 1) {
              targetState(args.subList(1, args.size))
            } else null
          }
    ```

=== "After"
    ```kotlin
      override fun updateTransition(args: NavStackList<T>): Transition<GestureNavTransitionHolder<T>> {
        val current = remember(args) { targetState(args) }
        val previous =
          remember(args) {
            val hasBackward = args.backwardItems.iterator().hasNext()
            if (hasBackward) {
              // Building the state we'd go to if we go backwards.
              val forward = listOf(args.active) + args.forwardItems
              val current = args.backwardItems.first()
              val backward = args.backwardItems.drop(1)
              targetState(navStackListOf(forward, current, backward))
            } else null
          }
    ```

## CircuitX

If you use `circuitx-navigation` there are additional changes:

### Updated implementations

The `NavigationInterceptor` interface now provides methods for handling forward/backward navigation:

```kotlin
class MyInterceptor : NavigationInterceptor {
  override fun forward(context: NavigationContext): InterceptedResult {
    return InterceptedResult.Skipped
  }
  
  override fun backward(context: NavigationContext): InterceptedResult {
    return InterceptedResult.Skipped
  }
}
```

The `NavigationEventListener` interface now provides methods for observing navstack changes as well as forward and backward navigation:

```kotlin
class MyEventListener : NavigationEventListener {
  override fun onNavStackChanged(navStack: NavStackList<*>, context: NavigationContext) {
    log("NavStack changed to $navStack")
  }

  override fun forward(context: NavigationContext) {
    log("Navigated forward")
  }

  override fun backward(context: NavigationContext) {
    log("Navigated backward")
  }
}
```


The `FailureNotifier` interface now provides methods for notifying forward/backward failures:
```kotlin
class MyFailureNotifier : InterceptingNavigator.FailureNotifier {
  override fun forwardFailure(interceptorResult: InterceptedResult.Failure) {
    log("Forward failed")
  }
  override fun backwardFailure(interceptorResult: InterceptedResult.Failure) {
    log("Backward failed")
  }
}
```


### Unified _Rewrite_ result

All interception methods can now use a unified `InterceptedResult.Rewrite`:

=== "Before"
    There was different rewrite types for different methods:

    ```kotlin
    override fun goTo(...): InterceptedGoToResult {
      return InterceptedGoToResult.Rewrite(NewScreen)
    }
    
    override fun resetRoot(...): InterceptedResetRootResult {
      return InterceptedResetRootResult.Rewrite(NewRoot, StateOptions.Default)
    }
    ```

=== "After"
    Now there is a Unified rewrite type that leverages `NavEvent`:
    ```kotlin
    override fun goTo(...): InterceptedResult {
      return InterceptedResult.Rewrite(NavEvent.GoTo(NewScreen))
    }
    
    override fun resetRoot(...): InterceptedResult {
      return InterceptedResult.Rewrite(
        NavEvent.ResetRoot(NewRoot, options = StateOptions.Default)
      )
    }
    
    // Can even rewrite to different navigation types
    override fun pop(...): InterceptedResult {
      // Rewrite a pop to a goTo instead
      return InterceptedResult.Rewrite(NavEvent.GoTo(SomeScreen))
    }
    ```

## FAQ

**Do I need to migrate?**

No, `BackStack` continues to work and now implements `NavStack`.

**What are the benefits of migrating?**

Forward/backward navigation, navigation snapshots, and enhanced decoration capabilities.

**Can I mix BackStack and NavStack?**

Yes, since `BackStack` implements `NavStack`, you can use them interchangeably.

**How does this affect state restoration?**

NavStack saves both position and forward history. BackStack behavior unchanged.

**Can I implement custom NavStack?**

Yes, implement the `NavStack` interface. See `SaveableNavStack` for reference.

**What happens to forward history when I push()?**

Forward history is truncated. Use `backward()` to preserve it.


## Resources

- **PR**: https://github.com/slackhq/circuit/pull/2501
- **Sample**: `samples/bottom-navigation` shows full `NavStack` usage
- **API Docs**: See `circuit-runtime-navigation` module
- **Migration Support**: File issues at https://github.com/slackhq/circuit/issues