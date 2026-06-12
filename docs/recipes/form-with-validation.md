# [Recipe](index.md): A form with validation and submit

**Problem:** a form has fields, per-field validation, a submit button that's only enabled when the
form is valid, and a submitting state.

Lift each field's value + error + validation into a small **presentation state holder** (the same idea
as `EmailFieldState` in [Scaling Presenters](../presenter-patterns.md#use-cases-separating-business-logic)).
The holder owns the Compose state with private setters and validates on every change; the presenter
just creates one per field with `rememberRetained` and reads from them.

## The field holder

A reusable `@Stable` holder: a value, a derived error, and validity. `onValueChange` re-validates.

```kotlin
@Stable
class FieldState(private val validate: (String) -> String?) {
  var value by mutableStateOf("")
    private set
  var error by mutableStateOf<String?>(null)
    private set

  val isValid: Boolean get() = value.isNotEmpty() && error == null

  fun onValueChange(newValue: String) {
    value = newValue
    error = validate(newValue)
  }
}
```

`validate` returns an error message or `null` — so a field is plug-and-play with any rule.

## The state and events

State exposes each field's value/error plus the derived `canSubmit` and `isSubmitting`. Events are
just "this field changed" and "submit".

```kotlin
@Stable
data class SignUpState(
  val email: String,
  val emailError: String?,
  val password: String,
  val passwordError: String?,
  val canSubmit: Boolean,
  val isSubmitting: Boolean,
  val eventSink: (SignUpEvent) -> Unit,
) : CircuitUiState

@Immutable
sealed interface SignUpEvent : CircuitUiEvent {
  data class EmailChanged(val value: String) : SignUpEvent
  data class PasswordChanged(val value: String) : SignUpEvent
  data object Submit : SignUpEvent
}
```

## The presenter

Create a `FieldState` per field with `rememberRetained` (so in-progress input survives rotation and
the back stack). `canSubmit` is **derived** from the holders, never stored.

Submission itself is **not** run from a `rememberCoroutineScope()` — account creation must finish even
if the user rotates or navigates away, so it lives in the data layer (scoped to outlive the screen).
The presenter just *triggers* it and *observes* its in-flight state, the same way it observes any
other repository state.

```kotlin
@Composable
override fun present(): SignUpState {
  val email = rememberRetained { FieldState(::validateEmail) }
  val password = rememberRetained { FieldState(::validatePassword) }

  // accountRepository owns the submission + its scope; we observe whether it's in flight.
  val submitting by produceRetainedState(initialValue = false) {
    accountRepository.signUpInFlight.collect { inFlight -> value = inFlight }
  }

  val canSubmit = email.isValid && password.isValid && !submitting

  return SignUpState(
    email = email.value,
    emailError = email.error,
    password = password.value,
    passwordError = password.error,
    canSubmit = canSubmit,
    isSubmitting = submitting,
  ) { event ->
    when (event) {
      is SignUpEvent.EmailChanged -> email.onValueChange(event.value)
      is SignUpEvent.PasswordChanged -> password.onValueChange(event.value)
      // Fire-and-trigger: the repository launches the work on its own scope and flips
      // signUpInFlight; no coroutine is launched from the presenter.
      SignUpEvent.Submit -> if (canSubmit) accountRepository.signUp(email.value, password.value)
    }
  }
}

private fun validateEmail(value: String): String? =
  if (value.isNotEmpty() && !value.isValidEmail()) "Enter a valid email" else null

private fun validatePassword(value: String): String? =
  if (value.isNotEmpty() && value.length < 16) "At least 16 characters" else null
```

## The UI

Binds values, surfaces errors, gates the button on `canSubmit`:

```kotlin
OutlinedTextField(
  value = state.email,
  onValueChange = { value -> state.eventSink(SignUpEvent.EmailChanged(value)) },
  isError = state.emailError != null,
  supportingText = { state.emailError?.let { error -> Text(error) } },
)
Button(onClick = { state.eventSink(SignUpEvent.Submit) }, enabled = state.canSubmit) {
  if (state.isSubmitting) CircularProgressIndicator() else Text("Sign up")
}
```

The validation logic lives in the holder, `canSubmit` is computed from the holders — there's no way
for them to drift out of sync with the field values, and adding a third field is one more
`rememberRetained { FieldState(...) }`. Submission runs in the data layer rather than from a
presenter coroutine scope, so it survives the user rotating or navigating away mid-request — see
[run a suspend action from an event](run-suspend-from-event.md) for why.

**See also:** [Scaling Presenters: state holders](../presenter-patterns.md#use-cases-separating-business-logic) ·
[States and Events](../states-and-events.md) ·
[Run a suspend action from an event](run-suspend-from-event.md)
