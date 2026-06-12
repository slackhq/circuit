# [Recipe](index.md): Return a result to the previous screen

**Problem:** you navigate to a screen (a picker, an editor) and need the value it produces back on
the calling screen — surviving process death.

Use the *answering navigator*. The caller wraps its `Navigator` with `rememberAnsweringNavigator<R>`
and a callback; the target screen pops a `PopResult`, and Circuit delivers it only to the caller that
asked.

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

## Screen result vs. overlay

If the thing you're asking for is an **ephemeral prompt** (confirm, pick from a sheet) rather than a
full destination, an [overlay](confirmation-dialog.md) is usually a better fit — it's type-safe and
suspends on the result, though it doesn't survive process death. Full comparison in the
[Overlays doc](../overlays.md#overlay-vs-popresult).

**See also:** [Navigation: results](../navigation.md#results) ·
[Ask for confirmation with a dialog](confirmation-dialog.md)
