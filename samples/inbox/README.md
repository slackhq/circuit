# Inbox

Inbox is a multiplatform sample for Android and Desktop that demonstrates a composite Circuit
presenter in an adaptive list-detail UI. The same presenter state renders as a single-pane flow on
phones and narrow windows, then expands into a two-pane layout on tablets, landscape devices, and
wide desktop windows.

This sample is the runnable companion for "Pattern 2: Composite Presenters" in
[docs/presenter-patterns.md](../../docs/presenter-patterns.md).

## What's Covered

- A composite presenter that embeds two real child `Presenter`s: `InboxListPresenter` and
  `EmailDetailPresenter`.
- Child presenters that do not know they are embedded. They only call
  `navigator.goTo()` and `navigator.pop()`, while the composite decides what those calls mean.
- An adaptive layout driven by `WindowSizeClass` in the UI layer. The presenter never knows how
  many panes are visible.
- Circuit's default `NavDecoration` for the compact list-detail transition, `AnimatedContent` for
  the layout swap, and `Modifier.animateItem()` for list changes.

## Running

- Desktop: `./gradlew :samples:inbox:run`
- Android: `./gradlew :samples:inbox:androidApp:installDebug`

## Structure

The sample registers three screens in one `Circuit`. `InboxScreen` is the root composite screen,
and the list and detail screens are still registered so they can be used on their own.

```
InboxPresenter   (composite, function-form, owns selectedEmailId)
├── InboxListPresenter      (class, @AssistedInject, also standalone-registered)
└── EmailDetailPresenter    (class, @AssistedInject, also standalone-registered)
```

The child presenters are ordinary `@AssistedInject` `Presenter` classes. They take `(Screen,
Navigator)` as assisted parameters, and they do not expose a special API for embedded use. Circuit
KSP registers those same classes for their standalone screens through `@CircuitInject` on each
nested `@AssistedFactory`.

| File                                                                                                                    | Role                                                                                                                                                                   |
|-------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`home/InboxPresenter.kt`](src/commonMain/kotlin/com/slack/circuit/sample/inbox/home/InboxPresenter.kt)                 | Composite presenter that owns selection, creates the child presenters from injected factories, and gives them a `SelectionNavigator`.                                  |
| [`home/InboxHomeUi.kt`](src/commonMain/kotlin/com/slack/circuit/sample/inbox/home/InboxHomeUi.kt)                       | Adaptive UI that chooses single-pane or two-pane rendering from `WindowSizeClass`, highlights the selected row, and preserves list scroll state across layout changes. |
| [`list/InboxListPresenter.kt`](src/commonMain/kotlin/com/slack/circuit/sample/inbox/list/InboxListPresenter.kt)         | List child presenter. It is an `@AssistedInject` class that takes a screen and navigator.                                                                              |
| [`detail/EmailDetailPresenter.kt`](src/commonMain/kotlin/com/slack/circuit/sample/inbox/detail/EmailDetailPresenter.kt) | Detail child presenter. It has the same standalone-friendly shape as the list presenter.                                                                               |

## Composing The Children

`InboxPresenter` injects both child `@AssistedFactory`s. To embed a child, it follows the same
path as a generated `Presenter.Factory`: create the presenter with a screen and navigator, then
call `present()`.

```kotlin
val navigator = remember { SelectionNavigator { emailId -> selectedEmailId = emailId } }
val listPresenter = inboxListFactory.create(InboxListScreen(EmailFolder.Inbox), navigator)
val listState = listPresenter.present()
```

The list presenter opens an email with `navigator.goTo(EmailDetailScreen(id))`. The detail
presenter goes back with `navigator.pop()`. When these presenters are used as standalone screens,
Circuit gives them the real navigator and those calls move the back stack.

Inside the composite, both children receive a `SelectionNavigator`. It does not mutate a back
stack. It is a small adapter at the composite boundary that translates the two navigation intents
this sample cares about into selection state.

```kotlin
private class SelectionNavigator(
  private val onSelectedEmailIdChanged: (String?) -> Unit,
) : Navigator by Navigator.NoOp {
  override fun goTo(screen: Screen): Boolean {
    if (screen !is EmailDetailScreen) return false

    onSelectedEmailIdChanged(screen.emailId)
    return true
  }

  override fun pop(result: PopResult?): Screen? {
    onSelectedEmailIdChanged(null)
    return null
  }
}
```

All other navigation calls fall through to `Navigator.NoOp`. The important part is that the child
presenters keep speaking normal Circuit navigation, while the composite controls how those calls
are interpreted in the embedded layout.

`InboxHomeUi` passes the current selected email id to `InboxListPane` in the two-pane layout so the
open conversation can be highlighted. The standalone list screen uses the default `null` selection,
so no row is highlighted there.

## Selection And Layout

`InboxPresenter` stores `selectedEmailId` in `rememberRetained`, so the selected conversation
survives configuration changes and window resizing. You can select an email, rotate a device or
drag the desktop window across the breakpoint, and the UI will move between single-pane and
two-pane rendering without losing the selection.

The presenter stays layout-agnostic. Only `InboxHomeUi.kt` reads `WindowSizeClass`, so the decision
about one pane versus two panes remains a rendering concern.

## Layout Animation

In compact mode, `InboxHomeUi` builds a tiny synthetic `NavStackList`: just `[List]` when no email
is selected, or `[Detail, List]` when one is open. It passes that stack to Circuit's configured
default `NavDecoration`, read from `LocalCircuit`, so the list-detail swap uses the same animation
system as a real `NavigableCircuitContent`.

When the window crosses the compact-to-expanded breakpoint, the outer layout cross-fades with
`AnimatedContent`. List mutations use `Modifier.animateItem()` with stable email ids, which gives
row insertions, removals, and moves a small amount of motion without adding presenter logic.

The list's `LazyListState` is hoisted above the layout swap with `rememberRetainedSaveable`, so
scroll position survives both layout changes and configuration changes.

## Dependency Injection

The sample uses [Metro](https://github.com/zacsweers/metro) and Circuit KSP, so the app does not
hand-write `Presenter.Factory` or `Ui.Factory` implementations.

- `EmailRepository` is `@Inject` and `@SingleIn(AppScope::class)`.
- Each child presenter is an `@AssistedInject` class with a nested `@AssistedFactory`. The factory
  carries `@CircuitInject(Screen::class, AppScope::class)`, which lets Circuit codegen register the
  standalone presenter factory.
- The composite is a `@CircuitInject`'d `@Composable fun`. Metro supplies the two child
  `@AssistedFactory`s from the graph.
- `InboxHomeUi`, `InboxList`, and `EmailDetail` are `@CircuitInject`'d `@Composable fun`s, and
  codegen registers their `Ui.Factory` implementations.
- `CircuitProviders` declares the multibound factory sets and builds the shared `Circuit`.
- `InboxAppGraph` is the platform-specific `@DependencyGraph` created by the Android and Desktop
  entry points.

## Email Data

[`EmailRepository`](src/commonMain/kotlin/com/slack/circuit/sample/inbox/data/EmailRepository.kt)
generates ~24 deterministic emails from Lorem Ipsum. The model separates storage location from
the UI folder:

- `EmailLocation` is where an email lives: `Inbox` or `Archive`.
- `EmailFolder` is what the UI shows: `Inbox`, `Starred`, or `Archive`.

`Starred` is a virtual folder. Starring an email does not move it out of its current location, so a
starred inbox email appears in both the Inbox tab and the Starred tab.

Repository state lives in a `MutableStateFlow<Map<String, Email>>`. Mutations like `markRead`,
`markUnread`, `toggleStar`, and `archive` update the flow, and observing presenters recompose from
the new values.

## Tests

`commonTest` has one test class per presenter.

- `InboxListPresenterTest` constructs the class directly with a `FakeNavigator` and verifies that
  clicking an email pushes the expected `EmailDetailScreen`.
- `EmailDetailPresenterTest` covers loading, missing email ids, starring, marking unread, archiving,
  and back navigation.
- `InboxPresenterTest` covers the composite behavior. It verifies that list clicks flow through
  `SelectionNavigator` into `detailState`, and that back and clear events remove the selection.

The composite tests build child factories from a small test-only `InboxTestGraph`. The graph takes
a test `EmailRepository` seed through its `@DependencyGraph.Factory`, which keeps the tests on the
same Metro-generated `@AssistedFactory` implementations as production without hand-written factory
stubs.
