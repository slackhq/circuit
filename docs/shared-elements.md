Shared Elements
===============

Circuit has an additional artifact for integrating [Compose Shared Elements Transitions](https://developer.android.com/develop/ui/compose/animation/shared-elements) with [Navigation](https://slackhq.github.io/circuit/navigation/) and [Overlays](https://slackhq.github.io/circuit/overlays/). Circuit Shared Elements are designed as a lightweight API to easily access the required `SharedTransitionScope` and `AnimatedVisibilityScope` directly in a Composable nested within a `Screen`. 

There are few core APIs for setting up and providing the required `SharedTransitionScope` in order use shared elements.

- [SharedElementTransitionLayout](#sharedelementtransitionlayout) is the layout that creates and provides a `SharedElementTransitionScope`
- [SharedElementTransitionScope](#sharedelementtransitionscope) is a `SharedTransitionScope` which is required to use the shared element modifiers. The `SharedElementTransitionScope` also provides access to a `AnimatedVisibilityScope`.


## Usage

### SharedElementTransitionLayout

Normally `SharedElementTransitionLayout` should be setup around the root Circuit. It needs to be outside of a `ContentWithOverlays` or `NavigableCircuitContent` in order for the _Overlay_ and _Navigation_ `AnimatedVisibilityScope` to be available. 

```kotlin
setContent {
  CircuitCompositionLocals(circuit) {
    SharedElementTransitionLayout {
      ContentWithOverlays { NavigableCircuitContent() }
    }
  }
}
```

A `PreviewSharedElementTransitionLayout` is provided for use when using [`@Preview`](https://developer.android.com/develop/ui/compose/tooling/previews) for any Composable that has a `SharedElementTransitionScope`.

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun PreviewShowAnimalPortrait() {
  PreviewSharedElementTransitionLayout {
    StarTheme { PetDetail(state = AnimalSuccessState, modifier = Modifier.fillMaxSize()) }
  }
}
```

### SharedElementTransitionScope

`SharedElementTransitionScope` extends `SharedTransitionScope` which is required to use the core shared elements API. The scope can be accessed using the `SharedElementTransitionScope` Composable wherever the `SharedTransitionScope` is needed, without having to explicitly pass it to a calling Composable. 


_Screen UI_
```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@CircuitInject(screen = HomeScreen::class)
@Composable
fun HomeContent(state: HomeScreen.State, modifier: Modifier = Modifier) = 
  SharedElementTransitionScope {
    // Content here
  }

```


_Normal UI_
```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GridItem(animal: PetListAnimal, modifier: Modifier = Modifier) = 
  SharedElementTransitionScope {
    // Content here
  }
```

Shared elements also need an `AnimatedVisibilityScope` in order to animate the shared Composable. Without having to explicitly pass the `AnimatedVisibilityScope` to the Composable, `SharedElementTransitionScope` has methods to access a `AnimatedVisibilityScope` via an [AnimatedScope](#animatedscope) key. This can be done with [`requireAnimatedScope`](https://slackhq.github.io/circuit/api/0.x/circuit-shared-elements/com.slack.circuit.sharedelements/-shared-element-transition-scope/index.html#-994011672%2FFunctions%2F1321375323) to require the  `AnimatedVisibilityScope` or with [`getAnimatedScope`](https://slackhq.github.io/circuit/api/0.x/circuit-shared-elements/com.slack.circuit.sharedelements/-shared-element-transition-scope/index.html#-1088686569%2FFunctions%2F1321375323) for an optional `AnimatedVisibilityScope`.

```kotlin
Box(
  modifier =
    Modifier.sharedElement(
      state = rememberSharedContentState(key = ImageElementKey(id)),
      animatedVisibilityScope = requireAnimatedScope(Navigation),
    )
)
```

```kotlin
Box(
  modifier =
    Modifier.thenIfNotNull(getAnimatedScope(Overlay)) { animatedScope ->
      sharedElement(
        state = rememberSharedContentState(key = ImageElementKey(id)),
        animatedVisibilityScope = animatedScope,
      )
    }
)
```


### AnimatedScope

By default Circuit provides a `Navigation` and `Overlay` `AnimatedVisibilityScope` when a `SharedElementTransitionScope` is available and either `NavigableCircuitContent` or `ContentWithOverlays` has been used. 

An `AnimatedScope` can be provided using `ProvideAnimatedTransitionScope`, including custom scopes. Doing so will make the `AnimatedVisibilityScope` available to the `SharedElementTransitionScope` inside the `ProvideAnimatedTransitionScope`. 

```kotlin
AnimatedContent(modifier = modifier, transitionSpec = transitionSpec()) { targetState ->
  ProvideAnimatedTransitionScope(Navigation, this) {
    AnimatedNavContent(targetState) { content(it) }
  }
}
```

In Circuit the _Navigation_ `AnimatedScope` is setup using a `ProvideAnimatedTransitionScope`. This is done with an implementation of `NavDecoration`, `AnimatedNavDecoration`. It takes an `AnimatedNavDecorator` to customize the Screen animation without having to manually setup a `AnimatedContent` and `SharedElementTransitionScope`. An example of a custom `AnimatedNavDecorator` can be seen with the `AndroidPredictiveBackNavigationDecoration` in [`circuitx-gesture-navigation`](https://slackhq.github.io/circuit/circuitx/#gesture-navigation).


## Non-Circuit usage

Circuit Shared Elements should be usable without any other Circuit artifact. The setup of `SharedElementTransitionLayout` is the same outside the root content, but a non-Circuit setup would require explicit use of `ProvideAnimatedTransitionScope` to provide the desired `AnimatedVisibilityScope`.
