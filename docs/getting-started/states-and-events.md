States and Events
=================

## Overview

The core state and event interfaces in Circuit are `CircuitUiState` and `CircuitUiEvent`. All state and event types should implement/extend these marker interfaces.

```kotlin
data class CounterState(val count: Int, val eventSink: (Event) -> Unit) : CircuitUiState

sealed interface CounterEvent : CircuitUiEvent {
  data object Increment : CounterEvent
  data object Decrement : CounterEvent
  data object Reset : CounterEvent
}
```

Presenters are simple classes or functions (usually the former) that compute state and handle events sent to them.

```kotlin
@Composable
fun CounterPresenter(): CounterState {
  var count by remember { mutableIntStateOf(0) }
  return CounterState(count) { event ->
    when (event) {
      Increment -> count++
      Decrement -> count--
      Reset -> count = 0
    }
  }
}
```

UIs are simple classes or functions (usually the latter) that render states. UIs can emit events via `eventSink` properties in state classes.

```kotlin
@Composable
fun Counter(state: CounterState, modifier: Modifier = Modifier) {
  Column(modifier) {
    Text("Count: ${state.count}")
    Button("Increment", onClick = { state.eventSink(Increment) })
    Button("Decrement", onClick = { state.eventSink(Decrement) })
    Button("Reset", onClick = { state.eventSink(Reset) })
  }
}
```

These are the core building blocks! States should be [`@Stable`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Stable); events should be [`@Immutable`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Immutable).

> Wait, event callbacks in state types?

Yep! This may feel like a departure from how you’ve written UDF patterns in the past, but we really like it. We tried different patterns before with event `Flow`s and having Circuit internals manage these for you, but we found they came with tradeoffs and friction points that we could avoid by just treating event emissions as another aspect of state. The end result is a tidier structure of state + event flows.

* Simpler cognitive overheads due to not always using `Flow` for events, which comes with caveats in compose (wrapping operators in `remember` calls, pipelining nested event flows, etc)
* Simple event-less UIs – state just doesn’t have an event sink.
* Simpler testing – no manual event flow needed. You end up writing more realistic tests where you tick along your presenter by emitting with its returned states directly.
* Different state types can have different event handling (e.g. `Click` may not make sense for `Loading` states).
* No internal ceremony around setting up a `Channel` and multicasting event streams.
* No risk of dropping events (unlike `Flow`).

!!! note
    Currently, while functions are treated as implicitly `Stable` by the compose compiler, they're not skippable when they're non-composable `Unit`-returning lambdas with equal-but-unstable captures. This may change though, and would be another free benefit for this case.

A longer-form writeup can be found in [this PR](https://github.com/slackhq/circuit/pull/146).

## FAQ

> Doesn't this break my state data classes' ability to use `equals()` in tests?

Yes, but that's ok! We found there are two primary solutions to this.

1. Granularly assert expected state property values, rather than the whole object at once.
2. Split your model into a separate class that is itself a property, if you really want/need to use `equals()` on the whole object. For example:
    ```kotlin
    data class StateData(val name: String, val age: Int)
    data class State(val data: StateData, val eventSink: (Event) -> Unit)
    ```

If neither of those satisfy your needs, there are alternative state designs described in [alternative designs](#alternative-designs) that avoid storing the event sink as a property.

> Don't lambdas break equality/stability checks? Do I need to wrap them in `remember` calls first?

Lambdas are automatically remembered in compose via [lambda memoization](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping#lambda-memoization), so you don't need to manually remember them first.

## Alternative Designs

The above docs describe how we conventionally write Circuit states. You're not limited to this however, and may want to write them differently depending on your project's needs. This section describes a few patterns we've explored. You can also mix-and match different aspects of these.

### Using [Poko](https://github.com/drewhamilton/Poko)

Poko is a neat library for generating hashCode/equals/toString impls without needing to use `data` classes. Aside from its documented benefits over data classes, it has a neat `@Poko.Skip` feature that allows for exclusion of annotated properties from equals/hashCode.

```kotlin
@Poko
class CounterState(val count: Int, @Poko.Skip val eventSink: (Event) -> Unit) : CircuitUiState
```

!!! tip "When to Use"
    Use this pattern if you want to limit API surface area from what data classes and want to just exclude event sinks from equals/hashCode.

### Poko with a shared event interface

If you want to take Poko a step further and avoid denoting it as a property at all, you can create a base interface that handles events.

```kotlin
@Stable
interface EventSink<UiEvent : CircuitUiEvent> {
  fun onEvent(event: UiEvent)
}

/**
 * Creates an [EventSink] that calls the given [body] when [EventSink.onEvent] is called.
 *
 * Note this inline function + [InlineEventSink] return type are a bit of bytecode trickery to avoid
 * creating a new class for every lambda passed to this function. The end result should be that the
 * lambda is inlined directly to the field in the implementing class and the inlined
 * [EventSink.onEvent] method impl is inlined directly as well to call it.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <UiEvent : CircuitUiEvent> eventSink(
  noinline body: (UiEvent) -> Unit
): EventSink<UiEvent> = InlineEventSink(body)

/** @see eventSink */
@PublishedApi
@JvmInline
internal value class InlineEventSink<UiEvent : CircuitUiEvent>(private val body: (UiEvent) -> Unit) : EventSink<UiEvent> {
  override fun onEvent(event: UiEvent) {
    body(event)
  }
}
```

!!! tip
    In this case, access from the UI is now `state.onEvent(<event>)`. You could change this function name to whatever you want, or even make it syntactically shorter with `operator fun invoke`.

With this, you can then define your state without marking the eventSink as a property.

```kotlin
@Poko
class CounterState(
  val count: Int,
  eventSink: (Event) -> Unit
) : CircuitUiState, EventSink<Event> by eventSink(eventSink)
```

!!! tip "When to Use"
    - You want to exclude event sinks from equals/hashCode without relying on `@Poko.Skip`.

### Using interfaces

Instead of defining a class for your state, you could define them in a more conventional compose-like state interface. Then, your presenters would return implementations of this interface that are backed directly by its internal `State` variables. Then, events are denoted as callable functions on the interface.

```kotlin
interface CounterState : CounterState {
  val count: Int
  fun increment() {}
  fun decrement() {}
}
```

Then its implementation in the presenter would look like so.

```kotlin
@Composable
fun CounterPresenter(): CounterState {
  return remember {
    object : CounterState {
      override var count: Int by mutableIntStateOf(0)
        private set

      override fun increment() {
        count++
      }

      override fun decrement() {
        count--
      }
    }
  }
}
```

!!! tip "When to Use"
    - You want to limit API surface area from what data classes
    - Want to exclude event sinks from equals/hashCode
    - Want to limit state object allocations (only one state instance is ever created then remembered).
        - Only do this if you have actually measured performance.
    - You want to bridge to another UDF architecture that uses event interfaces (super helpful for interop!)
