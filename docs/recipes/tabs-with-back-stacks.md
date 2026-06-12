# [Recipe](index.md): Tabs with independent back stacks

**Problem:** a bottom-navigation app where each tab keeps its own navigation history — switching tabs
and coming back lands you where you left off, not at the tab's root.

Make the tab host a Circuit UI that owns its own `NavStack`, `Navigator`, and
`NavigableCircuitContent`. Switch tabs with `resetRoot(..., StateOptions.SaveAndRestore)` so each
tab keeps its stack.

```kotlin
@Parcelize
data object HomeScreen : Screen

// One entry per tab. Each knows its root Screen and how to render in the bottom bar.
enum class HomeTab(val rootScreen: Screen, val label: String, val icon: ImageVector) {
  Feed(FeedScreen, "Feed", Icons.Default.Home),
  Search(SearchScreen, "Search", Icons.Default.Search),
  Profile(ProfileScreen, "Profile", Icons.Default.Person);

  companion object {
    fun of(rootScreen: Screen): HomeTab = entries.first { it.rootScreen == rootScreen }
  }
}
```

The UI owns the nested `NavStack` and `Navigator`, so tab switching can happen directly from the tab
click. The selected tab is derived from the nav stack's current root:

```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun Home(modifier: Modifier = Modifier) {
  // This tab host owns its own navigable surface.
  val navStack = rememberSaveableNavStack(HomeTab.entries.first().rootScreen)
  val navigator = rememberCircuitNavigator(navStack) {
    // Do something when the root screen is popped, usually exiting the app
  }

  // Highlight follows the stack root.
  val currentTab by remember {
    derivedStateOf { navStack.rootRecord?.screen?.let(HomeTab::of) ?: HomeTab.entries.first() }
  }

  Scaffold(
    modifier = modifier,
    bottomBar = {
      NavigationBar {
        HomeTab.entries.forEach { tab ->
          NavigationBarItem(
            selected = tab == currentTab,
            onClick = { navigator.resetRoot(tab.rootScreen, StateOptions.SaveAndRestore) },
            icon = { Icon(tab.icon, contentDescription = tab.label) },
            label = { Text(tab.label) },
          )
        }
      }
    },
  ) { padding ->
    NavigableCircuitContent(navigator, navStack, modifier = Modifier.padding(padding))
  }
}
```

`HomeScreen` can be a [`StaticScreen`](../ui.md#static-ui) because the UI owns the nested navigation
state. A presenter returning a trivial state works too; the tab logic still stays in the UI.

What this gives you:

- The nested `Navigator` and `BavStack` live inside the tab host.
- `SaveAndRestore` saves the outgoing tab's stack and restores the target tab's saved stack when it
  has one. Open **Feed**, drill into `DetailScreen`, switch to **Profile**, switch back, and
  `DetailScreen` is still on top.
- The first visit to a tab has nothing to restore, so it starts at that tab's root screen.

You do not need to track per-tab stacks or a separate "current tab" field. The nav stack root is the
selected tab, and `SaveAndRestore` stores the tab stacks.

## The full `StateOptions` API

`resetRoot` takes a [`Navigator.StateOptions`](https://slackhq.github.io/circuit/api/0.x/circuit-runtime/com.slack.circuit.runtime/-navigator/-state-options/),
a `data class` of three independent flags (all default `false`):

| Flag      | Effect                                                                                                                                                                    |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `save`    | Save the **current** entry list before resetting, keyed by the current root screen. It can be restored later by resetting back to that root with `restore = true`.        |
| `restore` | If the new root has previously-saved state, restore that whole stack instead of starting fresh. If `false` or there's nothing saved, the stack becomes just the new root. |
| `clear`   | Discard any saved state for the new root. Applied *after* a restore, regardless of `restore`.                                                                             |

Two presets cover the common cases:

| Preset                        | Equivalent                                  | Use                                      |
|-------------------------------|---------------------------------------------|------------------------------------------|
| `StateOptions.Default`        | `StateOptions()`                            | single back stack — save/restore nothing |
| `StateOptions.SaveAndRestore` | `StateOptions(save = true, restore = true)` | multiple back stacks (this recipe)       |

For anything else, construct one directly — e.g. save the outgoing stack but always start the new
tab fresh: `StateOptions(save = true, restore = false)`.

**See also:** [Navigation](../navigation.md) ·
[`StateOptions` API](https://slackhq.github.io/circuit/api/0.x/circuit-runtime/com.slack.circuit.runtime/-navigator/-state-options/) ·
the [bottom-navigation sample](https://github.com/slackhq/circuit/tree/main/samples/bottom-navigation)
