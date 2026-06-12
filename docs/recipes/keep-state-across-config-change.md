# [Recipe](index.md): Keep UI state across rotation and the back stack

**Problem:** scroll position, a half-typed field, an expanded/collapsed toggle — you want it to
survive rotation and back-stack navigation, and sometimes process death too.

Pick the API based on how durable the value must be:

| API                        | Survives rotation + back stack | Survives process death | Needs `Parcelable`/`Saver` |
|----------------------------|:------------------------------:|:----------------------:|:--------------------------:|
| `remember`                 |               ❌                |           ❌            |             –              |
| `rememberRetained`         |              ✅\*               |           ❌            |             no             |
| `rememberSaveable`         |              ✅\*               |           ✅            |            yes             |
| `rememberRetainedSaveable` |              ✅\*               |           ✅            |            yes             |

\* Back stack survival requires `NavigableCircuitContent`, which gives each record its own state
registry. `CircuitContent` alone does not offer a back stack to survive.

```kotlin
@Composable
override fun present(): EditorState {
  // Transient — fine to lose on rotation.
  var focusedField by remember { mutableStateOf<Field?>(null) }

  // Survives rotation + back stack, no serialization. Good default for most UI state.
  var expanded by rememberRetained { mutableStateOf(false) }

  // Also survives process death; needs a Saveable type (String is fine).
  var draft by rememberRetainedSaveable { mutableStateOf("") }

  // …
}
```

How to choose:

- **`remember`** — cheap, transient state you don't mind recomputing (focus, a transient highlight).
- **`rememberRetained`** — survives rotation and back-stack navigation by keeping the value in
  memory; no `Parcelable` needed. Use for selections, expanded states, and computed values you don't
  want to recompute.
- **`rememberSaveable`** — survives process death by serializing to instance state. Needs a
  `Parcelable` or custom `Saver`.
- **`rememberRetainedSaveable`** — keeps the value in memory like `rememberRetained`, and also saves
  it so the value can be restored after process death.

For primitives, use the boxing-free holders: `rememberRetained { mutableIntStateOf(0) }`,
`mutableLongStateOf`, etc.

!!! warning
    Retain *values*, not lifecycle-beholden objects like `Flow`, `Navigator`, or `Context`; those 
    can keep too much or leak across the back stack. To get data from a Flow into retained state 
    safely, see [observing a Flow](observe-a-flow.md).

**See also:** [Retention reference](../presenter.md#retention) · [Observe a Flow](observe-a-flow.md)
