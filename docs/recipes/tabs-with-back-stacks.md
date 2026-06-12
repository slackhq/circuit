# [Recipe](index.md): Tabs with independent back stacks

**Problem:** a bottom-navigation app where each tab keeps its own navigation history — switching tabs
and coming back lands you where you left off, not at the tab's root.

Make the tab host its **own Circuit UI** that owns a nested navigable surface: it sets up its own
`navStack` + `Navigator` internally and renders a `NavigableCircuitContent` for the current tab.
Switching tabs is a `resetRoot` with `StateOptions.SaveAndRestore`, which snapshots the outgoing
tab's stack and restores the incoming one.

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

The UI owns the nested `navStack`/`navigator`, so **tab switching is just a navigator call in the
`onClick`** — the click is the event. There's no presenter tab state and no
`LaunchedEffect(currentTab)` bridging state back to the navigator (which would re-run on every
replay/config change and clobber the restored stack). The selected tab is *derived* from the nav
stack's current root, so the bar always reflects reality:

```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun Home(modifier: Modifier = Modifier) {
  // This tab host owns its own navigable surface.
  val navStack = rememberSaveableNavStack(HomeTab.entries.first().rootScreen)
  val navigator = rememberCircuitNavigator(navStack) {
    // Do something when the root screen is popped, usually exiting the app
  }

  // Highlight follows the actual stack *root* — no separate "current tab" state to keep in sync.
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

`HomeScreen` needs no presenter — it's a [`StaticScreen`](../ui.md#static-ui) whose UI owns all its
own state. (If you'd rather not make it static, a presenter returning a trivial state works too; the
point is that none of the tab logic lives there.)

The mechanism:

- The nested `navigator` / `navStack` live **inside** the tab host, so the tabs are a self-contained
  navigable surface embedded in the screen — not wired up by the caller.
- Switching is a direct `resetRoot` in the `onClick`, so it fires exactly once per tap — no effect to
  replay. `SaveAndRestore` snapshots the current tab's stack before swapping roots and restores the
  target tab's saved stack if it has one. Open **Feed**, drill into `DetailScreen`, switch to
  **Profile**, switch back — `DetailScreen` is still on top.
- The first visit to a tab has nothing to restore, so it starts at that tab's `rootScreen`.

Don't track per-tab stacks — or even a "current tab" field — yourself. The nav stack root *is* the
selected tab, and `SaveAndRestore` is the whole feature.

## The full `StateOptions` API

`resetRoot` takes a [`Navigator.StateOptions`](https://slackhq.github.io/circuit/api/0.x/circuit-runtime/com.slack.circuit.runtime/-navigator/-state-options/),
a `data class` of three independent flags (all default `false`):

| Flag | Effect |
|------|--------|
| `save` | Save the **current** entry list before resetting, keyed by the current root screen. It can be restored later by resetting back to that root with `restore = true`. |
| `restore` | If the new root has previously-saved state, restore that whole stack instead of starting fresh. If `false` or there's nothing saved, the stack becomes just the new root. |
| `clear` | Discard any saved state for the new root. Applied *after* a restore, regardless of `restore`. |

Two presets cover the common patterns:

| Preset | Equivalent | Use |
|--------|------------|-----|
| `StateOptions.Default` | `StateOptions()` | single back stack — save/restore nothing |
| `StateOptions.SaveAndRestore` | `StateOptions(save = true, restore = true)` | multiple back stacks (this recipe) |

For anything else, construct one directly — e.g. save the outgoing stack but always start the new
tab fresh: `StateOptions(save = true, restore = false)`.

**See also:** [Navigation](../navigation.md) ·
[`StateOptions` API](https://slackhq.github.io/circuit/api/0.x/circuit-runtime/com.slack.circuit.runtime/-navigator/-state-options/) ·
the [bottom-navigation sample](https://github.com/slackhq/circuit/tree/main/samples/bottom-navigation)
