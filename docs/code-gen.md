Code Generation
===============

Circuit offers a KSP-based code gen solution to ease boilerplate around generating factories for several dependency injection tools.

## Installation

```kotlin
plugins {
  id("com.google.devtools.ksp")
}

dependencies {
  api("com.slack.circuit:circuit-codegen-annotations:<version>")
  ksp("com.slack.circuit:circuit-codegen:<version>")
}
```

Currently supported types are:
- [Anvil](https://github.com/square/anvil) and [Anvil KSP](https://github.com/zacsweers/anvil)
- [Dagger/Hilt](https://dagger.dev/hilt/)
- [kotlin-inject](https://github.com/evant/kotlin-inject) + [kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil)

Note that Dagger+Anvil is the default mode. 

If you are using another mode, you must specify the mode as a KSP arg.

```kotlin
ksp {
  arg("circuit.codegen.mode", "hilt") // or "kotlin_inject_anvil"
}
```

If using Kotlin multiplatform with typealias annotations for Dagger annotations (i.e. expect 
annotations in common with actual typealias declarations in JVM source sets), you can match on just 
annotation short names alone to support this case via `circuit.codegen.lenient` mode.

```kotlin
ksp {
  arg("circuit.codegen.lenient", "true")
}
```

If using anvil-ksp or kotlin-inject-anvil, you also need to indicate `@CircuitInject` as a 
contributing annotation.

```kotlin
ksp {
  // Anvil-KSP
  arg("anvil-ksp-extraContributingAnnotations", "com.slack.circuit.codegen.annotations.CircuitInject")
  // kotlin-inject-anvil (requires 0.0.3+)
  arg("kotlin-inject-anvil-contributing-annotations", "com.slack.circuit.codegen.annotations.CircuitInject")
}
```

## Usage

The primary entry point is the `CircuitInject` annotation.

This annotation is used to mark a UI or presenter class or function for code generation. When
annotated, the type's corresponding factory will be generated and keyed with the defined `screen`.

The generated factories are then contributed to Anvil via `ContributesMultibinding` and scoped
with the provided `scope` key.

## Classes

`Presenter` and `Ui` classes can be annotated and have their corresponding `Presenter.Factory` or
`Ui.Factory` classes generated for them.

**Presenter**
```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
class HomePresenter @Inject constructor(...) : Presenter<HomeState> { ... }

// Generates
@ContributesMultibinding(AppScope::class)
class HomePresenterFactory @Inject constructor() : Presenter.Factory { ... }
```

**UI**
```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
class HomeUi @Inject constructor(...) : Ui<HomeState> { ... }

// Generates
@ContributesMultibinding(AppScope::class)
class HomeUiFactory @Inject constructor() : Ui.Factory { ... }
```

## Functions

Simple functions can be annotated and have a corresponding `Presenter.Factory` generated. This is
primarily useful for simple cases where a class is just technical tedium.

**Requirements**
- Presenter function names _must_ end in `Presenter`, otherwise they will be treated as UI
functions.
- Presenter functions _must_ return a `CircuitUiState` type.
- UI functions can optionally accept a `CircuitUiState` type as a parameter, but it is not
required.
- UI functions _must_ return `Unit`.
- Both presenter and UI functions _must_ be `Composable`.

**Presenter**
```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun HomePresenter(): HomeState { ... }

// Generates
@ContributesMultibinding(AppScope::class)
class HomePresenterFactory @Inject constructor() : Presenter.Factory { ... }
```

**UI**
```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun Home(state: HomeState) { ... }
*
// Generates
@ContributesMultibinding(AppScope::class)
class HomeUiFactory @Inject constructor() : Ui.Factory { ... }
```

## Assisted injection

Any type that is offered in `Presenter.Factory` and `Ui.Factory` can be offered as an assisted
injection to types using Dagger `AssistedInject`. For these cases, the `AssistedFactory`
-annotated interface should be annotated with `CircuitInject` instead of the enclosing class.

Types available for assisted injection are:

- `Screen` – the screen key used to create the `Presenter` or `Ui`.
- `Navigator` – (presenters only)
- `Circuit`

Each should only be defined at-most once.

**Examples**
```kotlin
// Function example
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun HomePresenter(screen: Screen, navigator: Navigator): HomeState { ... }

// Class example
class HomePresenter @AssistedInject constructor(
  @Assisted screen: Screen,
  @Assisted navigator: Navigator,
  ...
) : Presenter<HomeState> {
  // ...
  @CircuitInject(HomeScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: Screen, navigator: Navigator, context: CircuitContext): HomePresenter
  }
}
```

### kotlin-inject

Assisted injection in kotlin-inject works slightly differently for classes. Since there is no 
`@AssistedFactory`, you can continue to just annotate the injected class directly.

```kotlin
@Inject
@CircuitInject(HomeScreen::class, AppScope::class)
class HomePresenter(
  @Assisted screen: Screen,
  @Assisted navigator: Navigator,
  ...
) : Presenter<HomeState>
```