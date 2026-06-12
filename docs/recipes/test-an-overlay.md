# [Recipe](index.md): Test a presenter that shows an overlay

**Problem:** your presenter shows an overlay (a confirm dialog, a picker) and acts on the result. You
want to test that logic without standing up real overlay UI.

The trick: don't try to drive a real `OverlayHost` in a unit test. If you model the overlay as
[nullable state](confirmation-dialog.md#gate-the-dialog-on-nullable-state-run-it-with-overlayeffect) —
`null` = hidden, non-null = showing — then "is the overlay requested?" and "what happens with each
result?" are just plain state-and-event assertions, exactly like any other presenter test.

```kotlin
@Test
fun `delete asks for confirmation, then deletes on confirm`() = runTest {
  val repository = FakeItemRepository(items = listOf(item1))
  val presenter = ItemPresenter(ItemScreen(item1.id), FakeNavigator(ItemScreen(item1.id)), repository)

  presenter.test {
    val loaded = assertIs<ItemState.Loaded>(awaitItem())
    assertNull(loaded.pendingDelete)                 // no dialog yet

    loaded.eventSink(ItemEvent.DeleteClicked(item1.id))

    // The presenter now requests the dialog by exposing pendingDelete.
    val confirming = assertIs<ItemState.Loaded>(awaitItem())
    assertEquals(item1.id, confirming.pendingDelete)

    // Simulate the user confirming — the same event the OverlayEffect would send.
    confirming.eventSink(ItemEvent.DeleteAnswered(item1.id, confirmed = true))

    assertTrue(repository.wasDeleted(item1.id))
  }
}

@Test
fun `dismissing the confirmation deletes nothing`() = runTest {
  val repository = FakeItemRepository(items = listOf(item1))
  val presenter = ItemPresenter(ItemScreen(item1.id), FakeNavigator(ItemScreen(item1.id)), repository)

  presenter.test {
    val loaded = assertIs<ItemState.Loaded>(awaitItem())
    loaded.eventSink(ItemEvent.DeleteClicked(item1.id))
    awaitItem()   // pendingDelete set

    loaded.eventSink(ItemEvent.DeleteAnswered(item1.id, confirmed = false))
    assertFalse(repository.wasDeleted(item1.id))
  }
}
```

Because the overlay's result re-enters the presenter as an ordinary event
(`DeleteAnswered(confirmed = …)`), you cover both the confirm and dismiss paths with no UI and no
fakes for the overlay machinery. The actual rendering is the `OverlayEffect`'s job and is exercised
by previews / snapshot tests, not unit tests.

**See also:** [Ask for confirmation with a dialog](confirmation-dialog.md) ·
[Test a presenter that navigates](test-a-presenter.md)
