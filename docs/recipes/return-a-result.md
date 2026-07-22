# [Recipe](index.md): Return a result to the previous screen

**Problem:** you navigate to a screen (a picker, an editor) and need the value it produces back on
the calling screen — surviving process death.

Call `rememberAnsweringNavigator<R>` with the caller's `Navigator` and handle the result in its callback. The target screen pops a `PopResult`, which Circuit delivers only to the caller that requested it. `NavigableCircuitContent` provides result delivery, and presenters can call the API through `circuit-runtime-presenter`.

## 1. Define the result

`PopResult` extends `Parcelable`, so results survive process death. `@Parcelize` is the easy way to
satisfy that.

```kotlin
@Parcelize
data object EditNameScreen : Screen

@Parcelize
data class EditNameResult(val name: String) : PopResult
```

## 2. Caller: request and receive

```kotlin
@Composable
override fun present(): ProfileState {
  var name by rememberRetained { mutableStateOf("") }

  val editNameNavigator =
    rememberAnsweringNavigator<EditNameResult>(navigator) { result ->
      name = result.name        // delivered here when the target pops
    }

  return ProfileState(name) { event ->
    when (event) {
      ProfileEvent.EditName -> editNameNavigator.goTo(EditNameScreen)
    }
  }
}
```

## 3. Target: pop the result

```kotlin
@Composable
override fun present(): EditNameState {
  var draft by rememberRetained { mutableStateOf("") }

  return EditNameState(draft) { event ->
    when (event) {
      is EditNameEvent.TextChanged -> draft = event.text
      EditNameEvent.Save -> navigator.pop(result = EditNameResult(draft))
    }
  }
}
```

If the target pops without a result (e.g. the user backs out), the callback simply never fires —
treat that as "cancelled".

Outside `NavigableCircuitContent`, `rememberAnsweringNavigator` returns the supplied fallback navigator and cannot deliver results. Check `answeringNavigationAvailable()` when a presenter needs different behavior in that case.

## Screen result vs. overlay

If the thing you're asking for is a prompt, such as confirm or pick from a sheet, an
[overlay](confirmation-dialog.md) is usually a better fit. It's type-safe and suspends on the
result, though it doesn't survive process death. Full comparison in the
[Overlays doc](../docs/overlays.md#overlay-vs-popresult).

**See also:** [Navigation: results](../docs/navigation.md#results) ·
[Ask for confirmation with a dialog](confirmation-dialog.md)
