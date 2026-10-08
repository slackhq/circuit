# `nav-stage`

A modular, layout-agnostic navigation decoration system for Circuit.

See the [user guide](https://slackhq.github.io/circuit/circuitx/nav-stage/) for usage, customization, and building your own stages.

---

## Overview

`nav-stage` decouples **navigation hierarchy** from **physical stage layout**. Your presenters emit pure, flat UI states, while `nav-stage` determines *how* and *where* to render them using layout strategies.



```mermaid
graph TD
    classDef main fill:#5050EC,stroke:#333,stroke-width:2px,color:#fff;
    classDef detail fill:#F5F5FA,stroke:#333,stroke-width:1px,color:#000;

    NavStackList --> NavStageStrategy
    NavStageStrategy -->|1. Resolves| NavStage
    NavStageDecoration -->|2. Renders Frame| NavStageFrame
    NavStageDecoration -->|3. Drives Stage Transitions| NavStageTransition
    NavStage -->|4. Renders Panes| NavStagePaneScope
    NavStagePaneScope -->|5. Animates Pane Content| PaneTransition

    class NavStageStrategy,NavStage,NavStageFrame,NavStageTransition,PaneTransition main;
```

1. **`NavStageStrategy`**: Evaluates the flat navigation stack (e.g., active screen, back stack history) and window metrics to determine the active layout stage.
2. **`NavStage`**: Holds layout structural logic (e.g., `SinglePaneNavStage`, `ListDetailNavStage`). Renders the physical layout container and mapping stack items to individual panes.
3. **`NavStageFrame`**: Applies styling decoration around the entire active stage (clipping, borders, shadows, background sheets).
4. **`NavStageTransition`**: Handles animations when transitioning *between stage layouts* (e.g., sliding or crossfading from single-pane to dual-pane when folding a device).
5. **`PaneTransition`**: Handles transitions *within an individual pane* when a new screen is pushed or popped inside a container (e.g., sliding a detail view in, while keeping the list view stable).

---

## Runtime Flow Sequence

Below is the runtime lifecycle of how a stack push/pop propagates through the layout decoration layers:

```mermaid
sequenceDiagram
    autonumber
    participant App as NavigableCircuitContent
    participant Deco as NavStageDecoration
    participant Strat as NavStageStrategy
    participant Stage as NavStage
    participant Frame as NavStageFrame
    participant Trans as NavStageTransition
    participant Pane as NavStagePaneScope

    App->>Deco: DecoratedContent(NavStackList, Navigator)
    Deco->>Strat: calculateStage(NavStackList)
    Strat-->>Deco: Returns Active NavStage (or fallback)
    Deco->>Frame: Frame.Content(stage, args)
    Frame->>Trans: AnimatedStageContent(targetState, stateFor, navigator)
    Note over Trans: Drives transition between stages<br/>(e.g., GestureNavStageTransition)
    Trans->>Stage: Stage.Content(args, paneScope)
    Stage->>Pane: paneScope.Pane(key, item, transition)
    Note over Pane: Renders individual screen content<br/>with localized PaneTransition
```



---

## Getting Started

### 1. Define Split-Screen Strategy
Define when your layout should split into dual-panes (list & detail) by providing predicates and pane-specific transitions. The predicates default to the `ListPane` and `DetailPane` marker interfaces, so screens implementing those need neither:

```kotlin
val listDetailStrategy = ListDetailNavStageStrategy(
  isListPane = { it is ListScreen },
  isDetailPane = { it is DetailScreen },
  listTransition = { PaneTransition.None }, // Keep list stable
  detailTransition = { PaneTransition.Default } // Slide+fade the detail pane
)
```

### 2. Configure `NavStageDecoration`
Provide the strategies to `NavStageDecoration` and set it as your navigation decorator in Circuit. Transitions like `GestureNavStageTransition` receive the explicit navigator parameter dynamically, keeping constructor scopes parameter-free and clean:

```kotlin
@OptIn(ExperimentalNavStageApi::class)
val decoration = NavStageDecoration(
  strategies = listOf(listDetailStrategy),
  stageTransition = GestureNavStageTransition()
)

CircuitCompositionLocals(circuit) {
  val navStack = rememberSaveableNavStack(HomeScreen)
  val navigator = rememberCircuitNavigator(navStack)
  NavigableCircuitContent(
    navigator = navigator,
    navStack = navStack,
    decoration = decoration
  )
}
```

---

## Shared Elements

`nav-stage` is fully integrated with standard Compose shared element transitions across Overlay, Stage, and individual Pane boundaries.

### Dynamic Stage Scope Resolution
To easily resolve the active `AnimatedVisibilityScope` in child screens (whether they are transitioning between screens inside a single pane or moving between stages), use the `findActiveStageScope()` extension on `SharedElementTransitionScope`:

```kotlin
SharedElementTransitionScope {
  val activeScope = findActiveStageScope()
  val heroModifier =
    if (activeScope != null) {
      Modifier.sharedElement(
        rememberSharedContentState(key = "hero-item"),
        animatedVisibilityScope = activeScope
      )
    } else Modifier
  Image(painter, contentDescription = null, modifier = heroModifier)
}
```

### Migrating from `AnimatedNavDecoration`
Under `NavStageDecoration` the `Navigation` scope belongs to the stage transition, so it only animates when the stage layout changes or a back gesture runs, not when a screen is pushed inside a pane. Every built-in `NavStageTransition` still provides it, so `requireAnimatedScope(Navigation)` won't throw, but shared elements keyed to it stop following in-pane navigation. Switch those to `findActiveStageScope()`, or to the `Pane` scope if they only care about in-pane navigation.
