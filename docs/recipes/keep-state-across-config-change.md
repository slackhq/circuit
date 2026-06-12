# [Recipe](index.md): Keep UI state across rotation and the back stack

**Problem:** scroll position, a half-typed field, an expanded/collapsed toggle — you want it to
survive rotation and back-stack navigation, and sometimes process death too.

Pick the retention tier by how durable the value must be:

| API | Survives rotation + back stack | Survives process death | Needs `Parcelable`/`Saver` |
|-----|:--:|:--:|:--:|
| `remember` | ❌ | ❌ | – |
| `rememberRetained` | ✅\* | ❌ | no |
| `rememberSaveable` | ✅\* | ✅ | yes |
| `rememberRetainedSaveable` | ✅\* | ✅ | yes |

\* Back-stack survival requires `NavigableCircuitContent` — it gives each record its own state
registry. Under a lone `CircuitContent` there's no back stack to survive (rotation and process death
still work as normal).

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
- **`rememberRetained`** — the workhorse. Survives rotation and back-stack navigation by keeping the
  value in memory; no `Parcelable` needed. Use for scroll-restorable selections, expanded states,
  computed values you don't want to recompute.
- **`rememberSaveable`** — survives process death by serializing to instance state. Needs a
  `Parcelable` or custom `Saver`.
- **`rememberRetainedSaveable`** — the union: in-memory like `rememberRetained` (fast, no
  serialize-on-every-config-change) **and** opportunistically saved so it survives process death. The
  in-memory value is the source of truth; the saved copy is the fallback when the process is recreated.

For primitives, use the boxing-free holders: `rememberRetained { mutableIntStateOf(0) }`,
`mutableLongStateOf`, etc.

!!! warning
    Retain *values*, never a `Flow`, `Navigator`, or `Context` — retaining those leaks everything
    they reference across the back stack. To get data from a Flow into retained state safely, see
    [observing a Flow](observe-a-flow.md).

**See also:** [Retention reference](../presenter.md#retention) · [Observe a Flow](observe-a-flow.md)
