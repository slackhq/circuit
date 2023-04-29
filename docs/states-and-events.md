States and Events
=================

The core state and event interfaces in Circuit are `CircuitUiState` and `CircuitUiEvent`. All state and event types should implement/extend these marker interfaces.

Presenters are simple functions that determine and return composable states. UIs are simple functions that render states. Uis can emit events via `eventSink` properties in state classes, which presenters then handle. These are the core building blocks!

States should be `@Stable`; events should be `@Immutable`.

> Wait, event callbacks in state types?

Yep! This may feel like a departure from how you’ve written UDF patterns in the past, but we really like it. We tried different patterns before with event `Flow`s and having Circuit internals manage these for you, but we found they came with tradeoffs and friction points that we could avoid by just treating event emissions as another aspect of state. The end result is a tidier structure of state + event flows.

* Simpler cognitive overheads due to not always using `Flow` for events, which comes with caveats in compose (wrapping operators in `remember` calls, pipelining nested event flows, etc)
* Simple event-less UIs – state just doesn’t have an event sink.
* Simpler testing – no manual event flow needed. You end up writing more realistic tests where you tick along your presenter by emitting with its returned states directly.
* Different state types can have different event handling (e.g. `Click` may not make sense for `Loading` states).
* No internal ceremony around setting up a `Channel` and multicasting event streams.
* No risk of dropping events (unlike `Flow`).

!!! warning
    Due to this [issue](https://issuetracker.google.com/issues/256100927), you need to extract the `eventSink` into local variables first.

!!! note
    Currently, while functions are treated as implicitly `Stable` by the compose compiler, they're not skippable when they're non-composable Unit-returning lambdas with equal-but-unstable captures. This may change though, and would be another free benefit for this case.

A longer-form writeup can be found in [this PR](https://github.com/slackhq/circuit/pull/146).
