# [Recipe](index.md): Run a one-shot suspend action from an event

**Problem:** a button tap needs to call a `suspend` function (save, send, toggle) — but a presenter's
`present()` isn't a coroutine, and you must not block it.

Launch from a `rememberCoroutineScope()` in the event handler:

```kotlin
@Composable
override fun present(): ComposerState {
  val scope = rememberCoroutineScope()
  var sending by rememberRetained { mutableStateOf(false) }

  return ComposerState(isSending = sending) { event ->
    when (event) {
      is ComposerEvent.Send ->
        scope.launch {
          sending = true
          messageRepository.send(event.text)
          sending = false
        }
    }
  }
}
```

## The catch: this scope dies with the composition

`rememberCoroutineScope()` is tied to the presenter's place in composition. It's **cancelled** the
moment the presenter leaves composition — a configuration change, the screen being popped, navigating
away. That's correct for short, UI-tied work (a quick save where, if the user leaves, abandoning it is
fine).

It is **wrong** for work that must finish regardless of the UI:

```kotlin
// 🚫 If the user navigates away mid-upload, this is cancelled and the upload is lost.
scope.launch { fileRepository.upload(hugeFile) }
```

Push fire-and-forget or must-complete work into the **data layer**, scoped to something that outlives
the screen (a repository/use-case that owns an application- or user-scoped coroutine scope):

```kotlin
// ✅ The repository owns a longer-lived scope; the presenter just triggers it.
is ComposerEvent.Send -> uploadManager.enqueue(event.file)
```

Rule of thumb: if the result still matters after the user leaves the screen, it doesn't belong on
`rememberCoroutineScope()`.

**See also:** [Pull to refresh](pull-to-refresh.md) · [Presenter](../presenter.md)
