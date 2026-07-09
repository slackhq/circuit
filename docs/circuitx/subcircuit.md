SubCircuit
==========

SubCircuit is a lightweight framework for rendering nested presenter/UI pairs that delegate events to an outer component rather than handling navigation themselves.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-subcircuit:<version>")
  // For code generation. `@SubCircuitInject` is handled by the same processor as `@CircuitInject`.
  ksp("com.slack.circuit:circuit-codegen:<version>")
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

SubCircuit uses KSP to generate factory classes that wire presenters and UIs into the DI graph. `@SubCircuitInject` is handled by the same `circuit-codegen` processor as `@CircuitInject`, so it shares the same setup and options — see [Circuit's code gen](../docs/code-gen.md).

!!! note "Migrating from `circuitx-subcircuit-codegen`"
    The old `circuitx-subcircuit-codegen` artifact is now a relocation pointer to `circuit-codegen`, so existing dependencies keep resolving, but you should depend on `circuit-codegen` directly. The `subcircuit.codegen.*` KSP options still work as fallbacks; prefer the `circuit.codegen.*` equivalents. Generated factories are behaviorally equivalent, though the exact source formatting now matches `@CircuitInject` output (named arguments, a `when (screen)` branch, and a `jakarta.inject.Inject` default — override with `circuit.codegen.useJavaxOnly`).

### Presenter Factories

Annotate your `@AssistedFactory` interface with `@SubCircuitInject`:

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

This generates a `SubPresenterFactory` implementation contributed to the DI graph via multibinding.

### UI Factories

Annotate `@Composable` UI functions directly:

```kotlin
@SubCircuitInject(ProfileCardScreen::class, AppScope::class)
@Composable
fun ProfileCardUi(state: ProfileCardState, modifier: Modifier = Modifier) {
  // ...
}
```

Requirements:

- Function must be `@Composable`
- First parameter must implement `SubCircuitUiState`
- Second parameter should be `modifier: Modifier`

### DI Modes

The code generator supports four DI frameworks via the `circuit.codegen.mode` KSP option:

=== "Anvil (default)"

    Generates `@ContributesMultibinding(Scope::class)` + `@Inject`.

=== "Hilt"

    Set `circuit.codegen.mode=hilt`. Generates a `@Module`/`@InstallIn` with a `@Binds @IntoSet` provider for the factory.

=== "kotlin-inject-anvil"

    Set `circuit.codegen.mode=kotlin_inject_anvil`. Generates `@Inject` + `@ContributesBinding(Scope::class, multibinding = true)`.

=== "Metro"

    Set `circuit.codegen.mode=metro`. Generates `@Inject` + `@ContributesIntoSet(Scope::class)`.

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
