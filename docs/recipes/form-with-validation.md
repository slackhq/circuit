# [Recipe](index.md): A form with validation and submit

**Problem:** a form has fields, per-field validation, a submit button that's only enabled when the
form is valid, and a submitting state.

Put each field's value, error, and validation into a small **presentation state holder** (the same idea
as `EmailFieldState` in [Scaling Presenters](../presenter-patterns.md#use-cases-separating-business-logic)).
The holder owns the Compose state and validates on every change; the presenter creates one per field
with `rememberRetained` and reads from them.

## The field holder

A reusable `@Stable` holder keeps the value/error/validity together. `onValueChange()`
re-validates.

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
  
  private fun validate(newValue: String): String? {
    // returns an error message or null, so each field can use its own rule
  }
}
```

## The state and events

State exposes each field's value and error, plus `canSubmit` and `isSubmitting`. Events are "this
field changed" and "submit".

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

Create a `FieldState` per field with `rememberRetained` so in-progress input survives rotation and
the back stack. Derive `canSubmit` from the holders instead of storing it separately.

Keep account creation in the data layer if it must persist through config changes or navigation.
The presenter triggers the request and observes its in-flight state like any other repository state.

```kotlin
@Composable
override fun present(): SignUpState {
  val email = rememberRetained { FieldState(::validateEmail) }
  val password = rememberRetained { FieldState(::validatePassword) }

  // accountRepository owns the submission; the presenter observes whether it is in flight.
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
      // The repository launches the work on its own scope and updates signUpInFlight.
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

The holder keeps validation close to the field value, and `canSubmit` is computed from the holders.
Adding another field is one more `rememberRetained { FieldState(...) }`. For more on where to launch
suspend work, see [run a suspend action from an event](run-suspend-from-event.md).

**See also:** [Scaling Presenters: state holders](../presenter-patterns.md#use-cases-separating-business-logic) ·
[States and Events](../states-and-events.md) ·
[Run a suspend action from an event](run-suspend-from-event.md)
