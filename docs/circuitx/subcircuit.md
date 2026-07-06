SubCircuit
==========

SubCircuit is a lightweight framework for rendering nested presenter/UI pairs that delegate events to an outer component rather than handling navigation themselves.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-subcircuit:<version>")
  // For code generation
  ksp("com.slack.circuit:circuitx-subcircuit-codegen:<version>")
  // For testing
  testImplementation("com.slack.circuit:circuitx-subcircuit-test:<version>")
}
```

!!! warning "Experimental"
    SubCircuit is annotated with `@ExperimentalSubCircuitApi` and may change without notice.

## When to Use SubCircuit

Use SubCircuit when you need nested, reusable UI components that:

- Don't need direct navigation access
- Delegate cross-cutting concerns (navigation, dialogs) to a parent
- Don't require `Parcelable` screen serialization
- Compose into larger Circuit screens as building blocks

Common use cases include list items that trigger navigation, embedded widgets reused across features, and renderable UI blocks that emit events to their container.

## SubCircuit vs Circuit

| Aspect | SubCircuit | Circuit |
|--------|------------|---------|
| Navigation | Delegated via `outerEventSink` | Direct via `Navigator` |
| Screen serialization | None required | `Parcelable` |
| Use case | Nested/embedded components | Top-level screens |
| DI wiring | `@SubCircuitInject` | `@CircuitInject` |

## Architecture

```text
┌─────────────────────────────────────────────────┐
│  Outer Circuit/Composable                       │
│  ┌───────────────────────────────────────────┐  │
│  │  outerEventSink: (OuterEvent) -> Unit     │  │
│  └─────────────────────▲─────────────────────┘  │
│                        │                        │
│                        │                        │
│  ┌─────────────────────┴───────────────────┐    │
│  │  SubCircuitContent                      │    │
│  │  ┌─────────────┐     ┌─────────────┐   │    │
│  │  │ SubPresenter│────▶│   SubUi     │   │    │
│  │  │             │     │             │   │    │
│  │  │ present()   │     │ Content()   │   │    │
│  │  └─────────────┘     └─────────────┘   │    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

## Core Types

| Type | Description |
|------|-------------|
| `SubScreen<OuterEvent>` | Marker interface for screens (no `Parcelable` requirement) |
| `SubPresenter<OuterEvent, State>` | Presenter that receives `outerEventSink` |
| `SubUi<State>` | UI component that renders state |
| `SubCircuit` | Registry for presenter and UI factories |
| `SubCircuitContent` | Composable that renders a SubScreen |
| `SubCircuitOuterEvent` | Marker for events delegated to the outer component |
| `SubCircuitUiState` | Marker for UI state types |
| `@SubCircuitInject` | Annotation for code generation |

## Usage

### 1. Define Outer Events

Events that need to be handled by the parent component:

```kotlin
sealed interface ProfileCardEvent : SubCircuitOuterEvent {
  data class NavigateToProfile(val userId: String) : ProfileCardEvent
  data class NavigateToChat(val userId: String) : ProfileCardEvent
}
```

### 2. Define UI State

```kotlin
data class ProfileCardState(
  val name: String,
  val avatarUrl: String?,
  val eventSink: (ProfileCardUiEvent) -> Unit
) : SubCircuitUiState

sealed interface ProfileCardUiEvent {
  data object Clicked : ProfileCardUiEvent
}
```

### 3. Create the SubScreen

```kotlin
data class ProfileCardScreen(
  val userId: String
) : SubScreen<ProfileCardEvent>
```

### 4. Implement the SubPresenter

```kotlin
class ProfileCardPresenter(
  private val screen: ProfileCardScreen,
  private val userRepository: UserRepository
) : SubPresenter<ProfileCardEvent, ProfileCardState> {

  @Composable
  override fun present(outerEventSink: (ProfileCardEvent) -> Unit): ProfileCardState {
    val user by produceState<User?>(null, screen.userId) {
      userRepository.getUser(screen.userId).collect { value = it }
    }

    return ProfileCardState(
      name = user?.name ?: "Loading...",
      avatarUrl = user?.avatarUrl,
      eventSink = { event ->
        when (event) {
          ProfileCardUiEvent.Clicked ->
            outerEventSink(ProfileCardEvent.NavigateToProfile(screen.userId))
        }
      }
    )
  }
}
```

### 5. Implement the UI

```kotlin
@SubCircuitInject(ProfileCardScreen::class, AppScope::class)
@Composable
fun ProfileCardUi(state: ProfileCardState, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.clickable { state.eventSink(ProfileCardUiEvent.Clicked) },
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(model = state.avatarUrl, modifier = Modifier.size(48.dp))
    Spacer(Modifier.width(12.dp))
    Text(text = state.name)
  }
}
```

### 6. Use SubCircuitContent

Render the SubCircuit from a parent composable, handling delegated events:

```kotlin
@Composable
fun TeamMembersUi(state: TeamMembersState, modifier: Modifier = Modifier) {
  LazyColumn(modifier = modifier) {
    items(state.members) { member ->
      SubCircuitContent(
        screen = ProfileCardScreen(userId = member.id),
        outerEventSink = { event ->
          when (event) {
            is ProfileCardEvent.NavigateToProfile ->
              state.eventSink(TeamMembersUiEvent.NavigateToProfile(event.userId))
            is ProfileCardEvent.NavigateToChat ->
              state.eventSink(TeamMembersUiEvent.NavigateToChat(event.userId))
          }
        }
      )
    }
  }
}
```

## Code Generation

SubCircuit uses KSP to generate factory classes that wire presenters and UIs into the DI graph. It mirrors [Circuit's code gen](../docs/code-gen.md), adapted to SubCircuit's contracts (screen-only factory `create`, no `presenterOf`).

Currently supported types are:

- [Anvil](https://github.com/square/anvil) and [Anvil KSP](https://github.com/zacsweers/anvil)
- [Dagger/Hilt](https://dagger.dev/hilt/)
- [kotlin-inject](https://github.com/evant/kotlin-inject) + [kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil)
- [Metro](https://github.com/ZacSweers/metro)

Note that Dagger+Anvil is the default mode.

If you are using another mode, you must specify the mode as a KSP arg.

```kotlin
ksp {
  arg("subcircuit.codegen.mode", "hilt") // or "kotlin_inject_anvil", "metro"
}
```

If using Kotlin multiplatform with typealias annotations for Dagger annotations (i.e. expect
annotations in common with actual typealias declarations in JVM source sets), you can match on just
annotation short names alone to support this case via `subcircuit.codegen.lenient` mode.

```kotlin
ksp {
  arg("subcircuit.codegen.lenient", "true")
}
```

If you need to generate `javax.inject` annotations instead of `jakarta.inject`, set
`subcircuit.codegen.useJavaxOnly`.

```kotlin
ksp {
  arg("subcircuit.codegen.useJavaxOnly", "true")
}
```

If using anvil-ksp or kotlin-inject-anvil, you also need to indicate `@SubCircuitInject` as a
contributing annotation.

```kotlin
ksp {
  // Anvil-KSP
  arg("anvil-ksp-extraContributingAnnotations", "com.slack.circuit.subcircuit.SubCircuitInject")
  // kotlin-inject-anvil (requires 0.0.3+)
  arg("kotlin-inject-anvil-contributing-annotations", "com.slack.circuit.subcircuit.SubCircuitInject")
}
```

### Presenter Factories

`SubPresenter` classes must be injectable — either annotate the class itself with `@Inject` (for
kotlin-inject and Metro) or annotate a constructor with `@Inject` (Dagger/Anvil/Hilt).

```kotlin
@SubCircuitInject(ProfileCardScreen::class, AppScope::class)
class ProfileCardPresenter @Inject constructor(
  private val userRepository: UserRepository
) : SubPresenter<ProfileCardEvent, ProfileCardState> {

  @Composable
  override fun present(outerEventSink: (ProfileCardEvent) -> Unit): ProfileCardState {
    // ...
  }
}
```

This generates a `SubPresenterFactory` implementation contributed to the DI graph via multibinding.

For assisted injection (e.g. injecting the `screen`), annotate the `@AssistedFactory` interface with
`@SubCircuitInject` instead of the enclosing class:

```kotlin
class ProfileCardPresenter @AssistedInject constructor(
  @Assisted val screen: ProfileCardScreen,
  private val userRepository: UserRepository
) : SubPresenter<ProfileCardEvent, ProfileCardState> {

  @Composable
  override fun present(outerEventSink: (ProfileCardEvent) -> Unit): ProfileCardState {
    // ...
  }

  @SubCircuitInject(ProfileCardScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(screen: ProfileCardScreen): ProfileCardPresenter
  }
}
```

The only assisted type SubCircuit provides is the `screen`. Unlike Circuit, there's no `Navigator`
or `CircuitContext` (navigation is delegated via `outerEventSink`).

In kotlin-inject, there's no `@AssistedFactory`, so continue to annotate the class directly:

```kotlin
@Inject
@SubCircuitInject(ProfileCardScreen::class, AppScope::class)
class ProfileCardPresenter(
  @Assisted val screen: ProfileCardScreen,
  private val userRepository: UserRepository,
) : SubPresenter<ProfileCardEvent, ProfileCardState>
```

### UI Factories

`SubUi` classes can be annotated the same way as presenters — annotate an injectable class:

```kotlin
@SubCircuitInject(ProfileCardScreen::class, AppScope::class)
class ProfileCardUi @Inject constructor() : SubUi<ProfileCardState> {
  @Composable
  override fun Content(state: ProfileCardState, modifier: Modifier) {
    // ...
  }
}
```

Or annotate `@Composable` UI functions directly:

```kotlin
@SubCircuitInject(ProfileCardScreen::class, AppScope::class)
@Composable
fun ProfileCardUi(state: ProfileCardState, modifier: Modifier = Modifier) {
  // ...
}
```

Requirements for UI functions:

- Function must be `@Composable`
- A `SubCircuitUiState` parameter is optional (omit it for static UI)
- A `modifier: Modifier` parameter is required

Unlike Circuit, function-based `SubPresenter`s are not supported. There's no `presenterOf` equivalent
in the runtime and `SubPresenter` isn't a `fun interface`, so presenters must be classes.

### Function-based injected dependencies

UI functions can accept injected dependencies directly as parameters. Any parameter that isn't a
SubCircuit-provided type (the state or `Modifier`) is treated as an injected dependency: the
generated factory accepts it as a provider (`Provider<T>` for Dagger/Anvil/Hilt, `() -> T` for
kotlin-inject and Metro) and invokes it once at `create()` time _outside_ the `SubUi { }` block (so
the provider isn't re-invoked on every recomposition).

Parameters that are already an indirect reference (`Provider<T>` (any flavor) or `Lazy<T>` (Dagger or
Kotlin)) are passed through as-is. In `metro` and `kotlin_inject_anvil` modes, `() -> T` is also
passed through; in Dagger/Anvil/Hilt modes it's wrapped in `Provider<() -> T>` like any other type.

### Qualifier propagation

Qualifier annotations (any annotation meta-annotated with `@Qualifier` like `javax.inject.Qualifier`,
`dev.zacsweers.metro.Qualifier`, etc.) are propagated from the `@SubCircuitInject`-annotated
declaration to the generated factory class.

### DI Modes

The generated annotations differ per mode:

=== "Anvil (default)"

    Generates `@ContributesMultibinding(Scope::class)` + `@Inject`.

=== "Hilt"

    Set `subcircuit.codegen.mode=hilt`. Generates a `@Module`/`@InstallIn` with a `@Binds @IntoSet` provider for the factory.

=== "kotlin-inject-anvil"

    Set `subcircuit.codegen.mode=kotlin_inject_anvil`. Generates `@Inject` + `@ContributesBinding(Scope::class, multibinding = true)`.

=== "Metro"

    Set `subcircuit.codegen.mode=metro`. Generates `@Inject` + `@ContributesIntoSet(Scope::class)`.

### Wiring

The generated factories are collected via multibinding and assembled into a `SubCircuit` instance:

```kotlin
@Provides
fun provideSubCircuit(
  presenterFactories: Set<@JvmSuppressWildcards SubPresenterFactory>,
  uiFactories: Set<@JvmSuppressWildcards SubUiFactory>
): SubCircuit =
  SubCircuit.builder()
    .addPresenterFactories(presenterFactories)
    .addUiFactories(uiFactories)
    .build()
```

Provide `SubCircuit` to the composition tree via `LocalSubCircuit`:

```kotlin
CompositionLocalProvider(LocalSubCircuit provides subCircuit) {
  // SubCircuitContent can now resolve screens
}
```

## Testing

The `circuitx-subcircuit-test` artifact provides a `.test {}` extension for SubPresenters, built on Molecule and Turbine.

```kotlin
@Test
fun presenterEmitsNavigationEvent() = runTest {
  val presenter = ProfileCardPresenter(
    screen = ProfileCardScreen("123"),
    userRepository = FakeUserRepository()
  )

  presenter.test {
    val state = awaitItem()
    assertEquals("Test User", state.name)

    state.eventSink(ProfileCardUiEvent.Clicked)

    assertEquals(
      ProfileCardEvent.NavigateToProfile("123"),
      outerEvents.awaitEvent()
    )
  }
}
```

### Test API

The `test {}` block provides a `SubCircuitReceiveTurbine` with:

- `awaitItem()` — Await the next distinct state emission (deduplicates unchanged states)
- `outerEvents.awaitEvent()` — Await the next outer event
- `outerEvents.assertNoEvents()` — Assert no outer events were emitted
- `awaitUnchanged()` — Assert the next emission is identical to the previous one

You can also provide a custom `TestOuterEventSink` or use `subPresenterTestOf` for more control over the composition:

```kotlin
@Test
fun customTestSetup() = runTest {
  val customSink = TestOuterEventSink<ProfileCardEvent>()

  subPresenterTestOf(
    presentFunction = { myPresenter.present(customSink::invoke) },
    outerEventSink = customSink
  ) {
    val state = awaitItem()
    // assertions...
  }
}
```
