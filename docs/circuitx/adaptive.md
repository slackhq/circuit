CircuitX provides a `NavDecoration` that shows one back stack as one, two, or three panes, based on
the window size. A list stays on screen and its detail opens beside it on tablets, open foldables,
and desktop windows.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-adaptive:<version>")
}
```

The back stack is the only navigation state. A fold, unfold, or rotation changes how the stack
renders, so screens keep their state.

!!! warning
    These APIs are experimental and need an opt-in to `ExperimentalCircuitAdaptiveApi`.

## Setup

Wrap your usual decoration in a `PaneNavDecoration`.

```kotlin
val host = remember { PaneHost() }
val decoration =
  remember(circuit, host) {
    PaneNavDecoration(
      strategy = ListDetailPaneStrategy(),
      delegate =
        AnimatedNavDecoration(
          animatedScreenTransforms = circuit.animatedScreenTransforms,
          decoratorFactory = GestureNavigationDecorationFactory(),
        ),
      host = host,
    )
  }
val paneNavigator =
  rememberInterceptingNavigator(
    navigator = navigator,
    interceptors = remember(host) { listOf(PaneReplaceInterceptor(host)) },
  )
NavigableCircuitContent(paneNavigator, backStack, decoration = decoration)
```

Then mark the screens that open beside the screen below them.

```kotlin
data class PetDetailScreen(val id: Long) : PaneScreen {
  override val paneStyle
    get() = PaneStyle.LIST_DETAIL
}
```

On a wide window, the left pane shows the list. The right pane passes the detail, and every screen
above it, to the wrapped decoration. Transitions and back gestures apply to the right pane only.

## Navigation rails and bars

Put app-wide navigation, like a navigation rail, outside `NavigableCircuitContent`. Material's
`NavigationSuiteScaffold` does this. The panes then share the width beside the rail, and the pane
widths need no changes.

If a screen inside the stack draws the rail, that screen is the left pane and holds two sized parts.
Let it size itself, as [Pane widths](#pane-widths) describes.

## Pane strategy

A `PaneStrategy` maps the window and the stack to a `PaneScene`, or to null for the single layout.
The scene names the records of the left and middle panes and how the panes split. The decoration
owns the layout, insets, record lifecycle, and animation scopes.

`ListDetailPaneStrategy` is the built-in strategy.

- It splits a window that has room for two panes and is at least 480 dp tall. Change the height with
  `minHeightDp`.
- The left pane takes the record just below the topmost run of screens that have a `PaneStyle`.
- `LIST_DETAIL` gives the left pane a set width. `SUPPORTING` gives the right pane a set width.
- `EXTRA` opens a third pane when the window has room for three. See [Three panes](#three-panes).
- The panes split at a vertical hinge. A window with a hinge shows two panes at most.
- A screen with no style, above the root, fills the window.

To keep feature modules free of this artifact, pass a lookup in place of the `PaneScreen` marker.

```kotlin
ListDetailPaneStrategy(
  paneStyle = { screen -> if (screen is PetDetailScreen) PaneStyle.LIST_DETAIL else null }
)
```

A custom strategy must return equal scenes for equal inputs. It must not read state that changes
during measure.

### Pane count and sizes

A `PaneWindow` tells the strategy how many panes fit (`maxPanes`) and how wide a pane is
(`preferredPaneWidth`). The default window takes both from Material's pane guidance.

| Window width       | Panes | Pane width |
|--------------------|-------|------------|
| Below 840 dp       | 1     |            |
| 840 dp to 1199 dp  | 2     | 360 dp     |
| 1200 dp and wider  | 3     | 412 dp     |

To use other values, pass a `window` function to `PaneNavDecoration`, or pass widths to
`ListDetailPaneStrategy`.

```kotlin
ListDetailPaneStrategy(listPaneWidth = 320.dp)
```

### Pane widths

A scene carries a `PaneSplit`, which tells the decoration where the left pane ends.

| Split          | Left pane width                                 |
|----------------|-------------------------------------------------|
| `Fixed`        | A width that the strategy chose                 |
| `AtHinge`      | Up to the first hinge, or half the window       |
| `PrimaryWraps` | The width that the left pane's content asks for |

Use `PrimaryWraps` when the left pane holds more than one sized part.
`ListDetailPaneStrategy(listPaneSizesItself = true)` returns it for `LIST_DETAIL`.

The decoration then measures the left pane with a minimum width of zero. The screen in that pane
must choose a width while `LocalPaneState.current.primarySizesItself` is true. Content that fills
the maximum width leaves no room beside it. `PaneState.preferredPaneWidth` is the width for a list.

```kotlin
val paneState = LocalPaneState.current
val sizesItself = paneState.primarySizesItself
Row(if (sizesItself) Modifier.fillMaxHeight() else Modifier.fillMaxSize()) {
  NavigationRail { /* ... */ }
  PetList(
    if (sizesItself) Modifier.width(paneState.preferredPaneWidth) else Modifier.weight(1f)
  )
}
```

### Three panes

A screen with `PaneStyle.EXTRA` holds more content for an open detail, like a profile opened from a
conversation. When the window has room for three panes, it opens in a third pane:

- The list stays in the left pane.
- The detail moves to a middle pane and keeps its state.
- The extra screen, and every screen above it, goes to the right pane.

In a smaller window, the extra screen stacks over the detail in the right pane.

A custom strategy asks for three panes with `PaneScene.secondaryKey` and `rightPaneWidth`.

## Roles

Each screen reads `LocalPaneRole` to learn where it renders.

| Role             | Where                                                     |
|------------------|-----------------------------------------------------------|
| `SINGLE`         | The only visible screen, as on phones                     |
| `PRIMARY`        | The left pane                                             |
| `DETAIL_ROOT`    | The screen opened from the left pane                      |
| `DETAIL_STACKED` | A screen above the detail                                 |
| `EXTRA_ROOT`     | The bottom screen of the third pane                       |
| `EXTRA_STACKED`  | A screen above another screen in the third pane           |

A screen with the `DETAIL_ROOT` role sits beside the screen that opened it. For a list's detail,
hide its back arrow there, because the list is already on screen. A supporting panel can show a
close button instead. Also turn off shared elements between panes. A list beside its detail stays
visible, so its items have nothing to share.

```kotlin
val showsBackArrow = LocalPaneRole.current != PaneRole.DETAIL_ROOT
val shareElements = LocalPaneRole.current == PaneRole.SINGLE
```

## Empty right pane

With only the left pane's record on the stack, that record fills the window. This suits a grid
that opens a panel on a pick. To keep the right pane visible, like for a list beside its detail,
set a placeholder. `ListDetailPaneStrategy` takes one for all screens. A screen can also
set one through `LocalPaneHost`.

```kotlin
host.placeholder = { Text("Select a pet") }
```

A screen that switches content, like a tab host, should set `placeholder` in the same event. The
next frame then lays out at the final widths.

`placeholder` is state. If you set it on each recomposition, set the same lambda instance.

## Pane state

A screen reads `LocalPaneState` to learn the layout of the window.

- `isSplit` is true while the window shows more than one pane.
- `detailRoot` is the screen opened from the left pane. A list can use it to highlight the open
  item.
- `preferredPaneWidth` is the pane width of the window.

```kotlin
val openItem = LocalPaneState.current.detailRoot as? PetDetailScreen
```

`PaneHost` has `isSplit` and `detailRoot` for code outside composition. It gets them after each
composition, so a screen that reads them there is one frame late.

## Replacing the detail

`PaneReplaceInterceptor` is a [`NavigationInterceptor`](navigation.md). While the window is split,
it applies these rules to `goTo`:

- A screen of a type that is already open beside the left pane replaces that screen and every
  screen above it.
- A screen that is already open there pops back to it.
- Any other screen is pushed.

The pops and the push are one `InterceptedResult.RewriteAll`, so the stack never renders between
them.

## Column counts

A list in a pane is narrower than the window. Size its columns from its own width, not from the
window size class.

```kotlin
LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Adaptive(minSize = 150.dp)) { /* ... */ }
```

## Writing your own pane decoration

`PaneNavDecoration` depends on the rules in the `NavDecoration` KDoc. The same rules apply to any
decoration that shows several records.

- Call `content` for a record from one place only. Two calls share saved state keys.
- Wrap records other than the current one in `ProvideRecordLifecycle`, or they stay paused.
- Wrap content outside the animated decoration in `ProvideStaticAnimatedTransitionScope`, so
  `requireAnimatedScope(Navigation)` does not throw.
- Choose the records of each pane in composition. Measure and place every child in every pass, with
  the same constraints in the lookahead pass and the main pass.
