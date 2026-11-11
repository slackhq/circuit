CircuitX provides a `NavDecoration` that shows one back stack as one or two panes, based on the
window size. A list stays on screen and its detail opens beside it on tablets, open foldables, and
desktop windows.

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

The decoration must fill the window for hinge bounds to line up with its panes.

## Pane strategy

A `PaneStrategy` maps the window and the stack to a `PaneScene`, or to null for the single layout.
The scene names the record in the left pane and how the panes split. The decoration owns the layout, insets,
record lifecycle, and animation scopes.

`ListDetailPaneStrategy` is the built-in strategy.

- It splits windows at least 840 dp wide and 480 dp tall. Change this with `minWidthDp` and
  `minHeightDp`.
- The left pane takes the record just below the topmost run of screens that have a `PaneStyle`.
- `LIST_DETAIL` gives the left pane a narrow width. `SUPPORTING` gives the right pane a narrow
  width.
- The panes split at a vertical hinge when both sides have room for a pane.
- A screen with no style, above the root, fills the window.

`ListDetailPaneDefaults` holds the default pane sizes.

### Pane widths

A scene carries a `PaneSplit`, which tells the decoration where the panes meet.

| Split          | Left pane width                           |
|----------------|-------------------------------------------|
| `Fixed`        | A width that the strategy computed        |
| `AtHinge`      | Up to the first hinge, or half the window |
| `PrimaryWraps` | The width that the left pane's content asks for |

Use `PrimaryWraps` when the left pane holds more than one sized part, like a navigation rail beside
a list. `ListDetailPaneStrategy(listPaneSizesItself = true)` returns it for `LIST_DETAIL`.

The decoration then measures the left pane with a minimum width of zero. The screen in that pane
must choose a width while `LocalPaneState.current.primarySizesItself` is true. Content that fills
the maximum width leaves the right pane empty. `Modifier.listPaneWidth()` gives a list the default
list pane width.

```kotlin
val sizesItself = LocalPaneState.current.primarySizesItself
Row(if (sizesItself) Modifier.fillMaxHeight() else Modifier.fillMaxSize()) {
  NavigationRail { /* ... */ }
  PetList(if (sizesItself) Modifier.listPaneWidth() else Modifier.weight(1f))
}
```

To keep feature modules free of this artifact, pass a lookup in place of the `PaneScreen` marker.

```kotlin
ListDetailPaneStrategy(
  paneStyle = { screen -> if (screen is PetDetailScreen) PaneStyle.LIST_DETAIL else null }
)
```

A custom strategy must return equal scenes for equal inputs. It must not read state that changes
during measure. Shared element transitions add a lookahead pass, and both passes need the same pane
widths.

## Roles

Each screen reads `LocalPaneRole` to learn where it renders.

| Role             | Where                                           |
|------------------|-------------------------------------------------|
| `SINGLE`         | The only visible screen, as on phones           |
| `PRIMARY`        | The left pane                                   |
| `DETAIL_ROOT`    | The bottom screen of the right pane             |
| `DETAIL_STACKED` | A screen above another screen in the right pane |

Use the role to show a close button in place of a back arrow, or to turn off shared elements
between panes. A list beside its detail stays visible, so its items have nothing to share.

```kotlin
val shareElements = LocalPaneRole.current == PaneRole.SINGLE
```

## Empty right pane

With only the left pane's record on the stack, that record fills the window. To keep the right pane
visible, set a placeholder. `ListDetailPaneStrategy` takes one for all screens. A screen can also
set one through `LocalPaneHost`.

```kotlin
host.placeholder = { Text("Select a pet") }
```

A screen that switches content, like a tab host, should set `placeholder` in the same event. The
next frame then lays out at the final widths.

`placeholder` is state. If you set it on each recomposition, set the same lambda instance.

## Pane state

A screen reads `LocalPaneState` to learn the layout of the window.

- `isSplit` is true while the window shows two panes. A tab host can use it to show a navigation
  rail.
- `detailRoot` is the screen at the bottom of the right pane. A list can use it to highlight the
  open item.

```kotlin
val showsRail = LocalPaneState.current.isSplit
```

`PaneHost` has the same values for code outside composition. It gets them after each composition, so
a screen that reads them there is one frame late.

## Replacing the detail

`PaneReplaceInterceptor` is a [`NavigationInterceptor`](navigation.md). While the window shows two
panes, it applies these rules to `goTo`:

- A screen of a type that is already in the right pane replaces that screen and every screen above
  it.
- A screen that is already in the right pane pops back to it.
- Any other screen is pushed.

The pops and the push are one `InterceptedResult.RewriteAll`, so the stack never renders between
them.

## Writing your own pane decoration

`PaneNavDecoration` depends on the rules in the `NavDecoration` KDoc. The same rules apply to any
decoration that shows several records.

- Call `content` for a record from one place only. Two calls share saved state keys.
- Wrap records other than the current one in `ProvideRecordLifecycle`, or they stay paused.
- Wrap content outside the animated decoration in `ProvideStaticAnimatedTransitionScope`, so
  `requireAnimatedScope(Navigation)` does not throw.
- Decide pane widths in composition. Measure and place every child in every pass.
