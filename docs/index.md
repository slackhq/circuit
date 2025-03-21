⚡️ Circuit
==========

Circuit is used in production at Slack and ready for general use 🚀. The API is considered unstable as we continue to iterate on it.

## Overview

Circuit is a simple, lightweight, and extensible framework for building Kotlin applications that's Compose from the ground up.

!!! info "Compose Runtime vs. Compose UI"
    Compose itself is essentially two libraries – Compose Compiler and Compose UI. Most folks usually think of Compose UI, but the compiler (and associated runtime) are actually not specific to UI at all and offer powerful state management APIs.
    
    Jake Wharton has an excellent post about this: https://jakewharton.com/a-jetpack-compose-by-any-other-name/

It builds upon core principles we already know like Presenters and UDF, and adds native support in its framework for all the other requirements we set out for above. It’s heavily influenced by Cash App’s Broadway architecture ([talked about at Droidcon NYC](https://www.droidcon.com/2022/09/29/architecture-at-scale/), also very derived from our conversations with them).

Circuit’s core components are its `Presenter` and `Ui` interfaces.

1. A `Presenter` and a `Ui` cannot directly access each other. They can only communicate through state and event emissions.
2. UIs are compose-first.
3. Presenters are _also_ compose-first. They do not emit Compose UI, but they do use the Compose runtime to manage and emit state.
4. Both `Presenter` and `Ui` each have a single composable function.
5. In most cases, Circuit automatically connects presenters and UIs.
6. `Presenter` and `Ui` are both generic types, with generics to define the `UiState` types they communicate with.
7. They are keyed by `Screen`s. One runs a new `Presenter`/`Ui` pairing by requesting them with a given `Screen` that they understand.

!!! note "Screens"
    The pairing of a `Presenter` and `Ui` for a given `Screen` key is what we semantically call a “screen".
    
    * Your application is composed of “screens".
    * A simple counter `Presenter` + `Ui` pairing would be a “counter screen".
    * Nested presenter/UIs would be “nested circuits” or “sub screen".
    * Composite presenter/UIs would be “composite screen".
    * etc etc.

Circuit’s repo (https://github.com/slackhq/circuit) is being actively developed in the open, which allows us to continue collaborating with external folks too. We have a trivial-but-not-too-trivial sample app that we have been developing in it to serve as a demo for a number of common patterns in Circuit use.

## Counter Example

This is a very simple case of a Counter screen that displays the count and has buttons to increment and decrement.

![image](https://user-images.githubusercontent.com/1361086/193662421-575dcaa9-4990-42e6-b265-9099a007296e.png)

There’s some glue code missing from this example that's covered in the [Code Gen](https://slackhq.github.io/circuit/code-gen/) section later.

```kotlin
@Parcelize
data object CounterScreen : Screen

data object CounterCircuit {
  data class CounterState(
    val count: Int,
    val eventSink: (CounterEvent) -> Unit,
  ) : CircuitUiState
  sealed interface CounterEvent : CircuitUiEvent {
    data object Increment : CounterEvent
    data object Decrement : CounterEvent
  }
}

@CircuitInject(CounterScreen::class, AppScope::class)
@Composable
fun CounterPresenter(): CounterState {
  var count by rememberSaveable { mutableStateOf(0) }

  return CounterState(count) { event ->
    when (event) {
      CounterEvent.Increment -> count++
      CounterEvent.Decrement -> count--
    }
  }
}

@CircuitInject(CounterScreen::class, AppScope::class)
@Composable
fun Counter(state: CounterState) {
  Box(Modifier.fillMaxSize()) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(CenterHorizontally),
        text = "Count: ${state.count}",
        style = MaterialTheme.typography.displayLarge
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        modifier = Modifier.align(CenterHorizontally),
        onClick = { state.eventSink(CounterEvent.Increment) }
      ) { Icon(rememberVectorPainter(Icons.Filled.Add), "Increment") }
      Button(
        modifier = Modifier.align(CenterHorizontally),
        onClick = { state.eventSink(CounterEvent.Decrement) }
      ) { Icon(rememberVectorPainter(Icons.Filled.Remove), "Decrement") }
    }
  }
}
```

License
--------

    Copyright 2022 Slack Technologies, LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
