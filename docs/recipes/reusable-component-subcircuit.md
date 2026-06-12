# [Recipe](index.md): Embed a reusable component that delegates navigation

**Problem:** you have a UI block reused across screens (a profile card, a list row, an embedded
widget) that emits events — but it shouldn't own navigation. The host screen should decide what a tap
means.

Use `SubCircuit` for embedded components. The child gets no `Navigator`; it sends navigation and other
host-owned events through an `outerEventSink`. That's almost always what you want when one screen's
content is composed *inside* another.

Pick based on how independent the component is
([background](https://github.com/slackhq/circuit/pull/2727#issuecomment-4636017793)):

| The component…                                                                                     | Tool                                                                                      |
|----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| is mostly standalone but defers navigation/dialogs to its host                                     | [`SubCircuit`](#subcircuit-the-default-for-embedding)                                     |
| must share live state with its siblings (selection mode, a shared filter)                          | hoist into the parent presenter — see [shared selection state](shared-selection-state.md) |
| is a fully independent destination that just happens to render here, and needs its own `Navigator` | bare [`CircuitContent`](#bare-circuitcontent-the-exception)                               |

## SubCircuit: the default for embedding

A `SubPresenter` receives an `outerEventSink` instead of a `Navigator`. The child needs no
`Parcelable` screen.

```kotlin
// 1. Outer events the parent handles, plus a SubScreen key.
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

The host renders it with `SubCircuitContent` and maps outer events to its own events:

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

This keeps the child reusable while leaving navigation decisions with the host.

## Bare CircuitContent: the exception

Use plain `CircuitContent` only when the embedded content is a fully independent destination that
needs its own `Navigator`, such as a self-contained section that navigates on its own.

```kotlin
@Composable
fun Dashboard(state: DashboardState, modifier: Modifier = Modifier) {
  Column(modifier) {
    CircuitContent(ProfileHeaderScreen(state.userId))   // runs its own presenter + UI, own nav
    CircuitContent(ActivityFeedScreen(state.userId))
  }
}
```

If you are reaching for `CircuitContent`'s `onNavEvent` overload to send child navigation up to the
parent, use `SubCircuit` instead.

**See also:** [SubCircuit](../circuitx/subcircuit.md) · [CircuitContent](../circuit-content.md) ·
[Share selection state across list items](shared-selection-state.md)
