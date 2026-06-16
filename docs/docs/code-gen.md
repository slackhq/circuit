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
- [Metro](https://github.com/ZacSweers/metro)

Note that Dagger+Anvil is the default mode. 

If you are using another mode, you must specify the mode as a KSP arg.

```kotlin
ksp {
  arg("circuit.codegen.mode", "hilt") // or "kotlin_inject_anvil", "metro"
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
`Ui.Factory` classes generated for them. The annotated class _must_ be injectable — either
annotate the class itself with `@Inject` (for kotlin-inject and Metro) or annotate a constructor
with `@Inject` (Dagger/Anvil/Hilt). Otherwise, the processor will fail with an error.

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

## Qualifier propagation

Qualifier annotations (any annotation meta-annotated with `@Qualifier` like `javax.inject.Qualifier`,
`dev.zacsweers.metro.Qualifier`, etc.) are propagated from the `@CircuitInject`-annotated 
declaration to the generated factory class.

```kotlin
@Named("home")
@Inject
@CircuitInject(HomeScreen::class, AppScope::class)
class HomePresenter(...) : Presenter<HomeState>

// Generates
@Inject
@ContributesIntoSet(AppScope::class)
@Named("home")
class HomePresenterFactory(...) : Presenter.Factory { ... }
```

## Function-based injected dependencies

Function-based presenters and UIs can accept any injected dependency directly as a parameter. Any
parameter type that isn't one of the circuit-provided types (see [Assisted
injection](#assisted-injection)) is treated as a regular injected dependency and hoisted: the generated
factory accepts it as a provider (`Provider<T>` for Dagger/Anvil/Hilt, `() -> T` for
kotlin-inject and Metro) and invokes it once at `create()` time _outside_ the
`presenterOf { }`/`ui { }` block (so the provider isn't re-invoked on every recomposition).

Parameters that are already an indirect reference to a dependency (`Provider<T>` (any flavor)
or `Lazy<T>` (Dagger or Kotlin)) are passed through to the factory constructor as-is rather
than being re-wrapped in another provider. In `metro` and `kotlin_inject_anvil` modes, `() -> T`
is also treated as a provider and passed through; in Dagger/Anvil/Hilt modes it is treated as a
regular dependency and wrapped in `Provider<() -> T>` like any other type.

```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
@Inject
@Composable
fun HomePresenter(
  navigator: Navigator,       // circuit-provided
  repository: UserRepository, // injected — not recognized as circuit-provided, so treated as a dependency
): HomeState { ... }

// Generates (metro mode shown)
@Inject
@ContributesIntoSet(AppScope::class)
class HomePresenterFactory(
  private val repository: () -> UserRepository,
) : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ): Presenter<*>? = when (screen) {
    HomeScreen -> {
      val repository = repository()
      presenterOf { HomePresenter(navigator = navigator, repository = repository) }
    }
    else -> null
  }
}
```

Class-based presenters and UIs don't need this special handling, since constructor parameters
there are already unambiguously injected dependencies.