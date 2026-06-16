Setting up Circuit
==================

Setting up Circuit is a breeze! Just add the following to your build:

## Installation

The simplest way to get up and running is with the `circuit-foundation` dependency, which includes all the core Circuit artifacts.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuit-foundation:<version>")
}
```

## Setup

Create a `Circuit` instance. This controls all your common configuration, Presenter/Ui factories, etc.

```kotlin
val circuit = Circuit.Builder()
  .addUiFactory(AddFavoritesUiFactory())
  .addPresenterFactory(AddFavoritesPresenterFactory())
  .build()
```

This configuration can be rebuilt via `newBuilder()` and usually would live in your program's DI graph.

Once you have a configuration ready, the simplest way to get going with Circuit is via `CircuitCompositionLocals`. This automatically exposes the config to all child Circuit composables and allows you to get off the ground quickly with `CircuitContent`, `NavigableCircuitContent`, etc.

```kotlin
CircuitCompositionLocals(circuit) {
  CircuitContent(AddFavoritesScreen())
}
```

See the docs for `CircuitContent` and `NavigableCircuitContent` for more information.

## Granular Artifacts

Circuit is split into a few different artifacts to allow for more granular control over your dependencies. The following table shows the available artifacts and their purpose:

| Artifact ID                 | Dependencies                                                                                     |
|-----------------------------|--------------------------------------------------------------------------------------------------|
| `circuit-backstack`         | Circuit's backstack implementation.                                                              |
| `circuit-runtime`           | Common runtime components like `Screen`, `Navigator`, etc.                                       |
| `circuit-runtime-presenter` | The `Presenter` API, depends on `circuit-runtime`.                                               |
| `circuit-runtime-ui`        | The `Ui` API, depends on `circuit-runtime`.                                                      |
| `circuit-foundation`        | The Circuit foundational APIs like `Circuit`, `CircuitContent`, etc. Depends on the first four. |
| `circuit-test`              | First-party test APIs for testing navigation, state emissions, and event sinks.                  |
| `circuit-overlay`           | Optional `Overlay` APIs.                                                                         |
| `circuit-retained`          | Optional `rememberRetained()` APIs.                                                              |

## Platform Support

Circuit is a multiplatform library, but not all features are available on all platforms. The following table shows which features are available on which platforms:

- ✅ Available
- ❌ Not available
- – Not applicable

| Feature                    | Android | JVM | iOS | JS | Notes                                       |
|----------------------------|---------|-----|-----|----|---------------------------------------------|
| `Backstack`                | ✅       | ✅   | ✅   | ✅  |                                             |
| `CircuitContent`           | ✅       | ✅   | ✅   | ✅  |                                             |
| `ContentWithOverlays`      | ✅       | ✅   | ✅   | ✅  |                                             |
| `NavigableCircuitContent`  | ✅       | ✅   | ✅   | ✅  |                                             |
| `Navigator`                | ✅       | ✅   | ✅   | ✅  |                                             |
| `SaveableBackstack`        | ✅       | ✅   | ✅   | ✅  | Saveable is a no-op on non-android.         |
| `rememberCircuitNavigator` | ✅       | ✅   | ✅   | ✅  |                                             |
| `rememberRetained`         | ✅       | ✅   | ✅   | ✅  |                                             |
| `TestEventSink`            | ✅       | ✅   | ✅   | ✅  | On JS you must use `asEventSinkFunction()`. |
