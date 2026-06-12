# [Recipe](index.md): Embed a reusable component that delegates navigation

**Problem:** you have a UI block reused across screens (a profile card, a list row, an embedded
widget) that emits events — but it shouldn't own navigation. The host screen should decide what a tap
means.

**Reach for SubCircuit.** It's purpose-built for embedded components: the child gets no `Navigator`
and delegates everything it can't handle (navigation, dialogs) to its host through an
`outerEventSink`. That's almost always what you want when one screen's content is composed *inside*
another.

Pick by how coupled the component is
([background](https://github.com/slackhq/circuit/pull/2727#issuecomment-4636017793)):

| The component… | Tool |
|----------------|------|
| is mostly standalone but defers navigation/dialogs to its host | [`SubCircuit`](#subcircuit-the-default-for-embedding) ← **default** |
| must share live state with its siblings (selection mode, a shared filter) | hoist into the parent presenter — see [shared selection state](shared-selection-state.md) |
| is a fully independent destination that just happens to render here, and needs its own `Navigator` | bare [`CircuitContent`](#bare-circuitcontent-the-exception) |

## SubCircuit: the default for embedding

A `SubPresenter` gets no `Navigator` — it receives an `outerEventSink` and pushes anything it can't
handle up to the parent. The child needs no `Parcelable` screen.

```kotlin
// 1. Outer events the parent handles, and a SubScreen key (no Parcelable needed).
sealed interface ProfileCardOuterEvent : SubCircuitOuterEvent {
  data class OpenProfile(val userId: String) : ProfileCardOuterEvent
}

data class ProfileCardScreen(val userId: String) : SubScreen<ProfileCardOuterEvent>

// 2. SubPresenter receives outerEventSink instead of a Navigator.
class ProfileCardPresenter(private val screen: ProfileCardScreen) :
  SubPresenter<ProfileCardOuterEvent, ProfileCardState> {

  @Composable
  override fun present(outerEventSink: (ProfileCardOuterEvent) -> Unit): ProfileCardState =
    ProfileCardState(userId = screen.userId) { event ->
      when (event) {
        ProfileCardUiEvent.Clicked ->
          outerEventSink(ProfileCardOuterEvent.OpenProfile(screen.userId))
      }
    }
}
```

The host renders it with `SubCircuitContent` and maps the outer events onto its own:

```kotlin
@Composable
fun TeamMembers(state: TeamMembersState, modifier: Modifier = Modifier) {
  LazyColumn(modifier) {
    items(state.members, key = { member -> member.id }) { member ->
      SubCircuitContent(
        screen = ProfileCardScreen(member.id),
        outerEventSink = { outerEvent ->
          when (outerEvent) {
            is ProfileCardOuterEvent.OpenProfile ->
              state.eventSink(TeamMembersEvent.OpenProfile(outerEvent.userId))
          }
        },
      )
    }
  }
}
```

This is the same "curry the child's events up to the parent" idea as `CircuitContent`'s `onNavEvent`
overload — SubCircuit just formalizes it and removes the child's `Navigator` entirely.

## Bare CircuitContent: the exception

Only reach for plain `CircuitContent` when the embedded thing is a **fully independent destination**
that needs its own `Navigator` — e.g. a self-contained section that navigates on its own and merely
happens to render inside this screen. It gives the child the ambient `Navigator`, which is exactly
what you *don't* want for a delegating component.

```kotlin
@Composable
fun Dashboard(state: DashboardState, modifier: Modifier = Modifier) {
  Column(modifier) {
    CircuitContent(ProfileHeaderScreen(state.userId))   // runs its own presenter + UI, own nav
    CircuitContent(ActivityFeedScreen(state.userId))
  }
}
```

If you find yourself reaching for `CircuitContent`'s `onNavEvent` overload to curry a child's
navigation up to the parent, that's the signal you actually wanted SubCircuit.

**See also:** [SubCircuit](../circuitx/subcircuit.md) · [CircuitContent](../circuit-content.md) ·
[Share selection state across list items](shared-selection-state.md)
