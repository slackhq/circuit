# [Recipe](index.md): Test a presenter that navigates

**Problem:** verify that a presenter emits the right states and navigates where you expect, in
response to events.

Use `Presenter.test {}` from `circuit-test` and a `FakeNavigator`. `test {}` gives you a `Turbine` for
presenter states. Drive the presenter by invoking the `eventSink` on an emitted state.

```kotlin
@Test
fun `clicking an item navigates to its detail`() = runTest {
  val navigator = FakeNavigator(FeedScreen)
  val presenter = FeedPresenter(FeedScreen, navigator, FakeFeedRepository(items = listOf(item1)))

  presenter.test {
    // Consume the initial loading emission before asserting on loaded state.
    assertEquals(FeedState.Loading, awaitItem())

    val loaded = assertIs<FeedState.Loaded>(awaitItem())
    loaded.eventSink(FeedEvent.OpenItem(item1.id))     // simulate the tap

    assertEquals(DetailScreen(item1.id), navigator.awaitNextScreen())
  }
}
```

Common `FakeNavigator` assertions:

- `awaitNextScreen()` — the next screen pushed via `goTo`
- `awaitPop()` — a `pop()` happened
- `awaitResetRoot()` — a `resetRoot()` happened
- `assertGoToIsEmpty()` — assert *no* navigation occurred

Things to watch for:

- **Consume `Loading` first.** A presenter that starts in `Loading` emits it before the loaded state.
  `assertIs<FeedState.Loaded>(awaitItem())` on the *first* item will fail — await the loading emission first.
- **`awaitItem()` is distinct-until-changed.** Identical consecutive states collapse into one, so you
  assert state changes, not every recomposition. When you specifically want to assert a
  recomposition produced *no* change, use the escape hatch `awaitUnchanged()` — it awaits the next
  emission and fails if it differs from the previous one.

For UI-level event assertions, render the composable with a `TestEventSink` and assert the events it
emits — see the [testing doc](../testing.md#android-ui-instrumentation-tests).

**See also:** [Testing](../testing.md) · [Test a presenter that shows an overlay](test-an-overlay.md)
