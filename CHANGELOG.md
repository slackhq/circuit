Changelog
=========

Unreleased
----------

0.33.1
------

_2026-02-19_

### New

- Added `ProvideRecordLifecycle` to safely override the `LocalRecordLifecycle` behaviour of `NavigableCircuitContent` where only the currently active record in the `NavStack` is not paused.
- Add `rememberSaveableNavStack(NavStackList)` overload to initialize a nav stack from an existing snapshot.
- Added a `Presenter.test` variant that provides composition local values to the `Presenter` under test

### Fixes

- Fixes an issue where retained values where not getting saved at the correct time.

### Docs

- New ["Scaling Presenters"](https://slackhq.github.io/circuit/presenter-patterns/) guide covering best practices for structuring presenters as they grow, including extracting sub-presenters, modularizing event handling, and testing strategies.


### Contributors

Special thanks to the following contributors for contributing to this release!

- [@matthewbahr-clear](https://github.com/matthewbahr-clear)


0.33.0
------

_2026-02-10_

### New Navigation Architecture:

Circuit now supports **bidirectional navigation** with browser-style forward/backward capabilities!

```kotlin
val navStack = rememberSaveableNavStack(root = HomeScreen)
val navigator = rememberCircuitNavigator(navStack)
navigator.backward() // Move back without removing history
navigator.forward()  // Move forward to a previously visited screen
```

**Navigation changes**:

- `Navigator.forward()`: Move forward in navigation history
- `Navigator.backward()`: Move backward in navigation history
- `Navigator.peekNavStack()`: Immutable snapshot of the current navigation stack state
- `NavigableCircuitContent` is aware of the full navigation stack and provides `NavStackList` to
  decorations, enabling them to render forward stack records.

**SaveableNavStack**:

- New implementation in `circuit-foundation` providing full bidirectional navigation state
- The existing `SaveableBackStack` implementation has been updated to extend `NavStack`

**New `circuit-runtime-navigation` artifact**:

- `NavStack`: Core navigation stack supporting push/pop and forward/backward traversal
- `NavStackList`: Immutable snapshot of navigation state

**circuitx-navigation**:

- `InterceptingNavigator` now supports `forward()` and `backward()` navigation methods
- Updated to use `NavStack` and `NavStackList` instead of `BackStack` and `List<Screen>`
- Rewrite interceptors can now rewrite to any `NavEvent` (not just specific navigation types like `InterceptedGoToResult.Rewrite` or `InterceptedResetRootResult.Rewrite`)
- `FailureNotifier` interface updated with `forwardFailure()` and `backwardFailure()` methods with default implementations

### New

- Add `mingwX64()` target to `circuit-codegen-annotations`.

### Fixes

- Fix an issue where `AnsweringResultHandler` was not correctly parceling pending results.

### Changes

- Compile against kotlin-inject-anvil `0.1.7`.
- `NavigationEventHandler` usage is now disabled by default when no `NavigationEventDispatcherOwner` is available, preventing crashes in environments with dynamic navigation event support. This behaviour can be configured with the `Circuit.lenientNavigationEventDispatcherOwner` option.
- `BottomSheetOverlay` now exposes `contentWindowInsets`, mirroring `ModalBottomSheet`

### Contributors

Special thanks to the following contributors for contributing to this release!

- [@spectrl](https://github.com/spectrl)

0.32.0
------

_2026-01-13_

### Enhancements
- New `produceAndCollectAsRetainedState` that produces and collects values from a `Flow`.

### Changes

- Update to Kotlin `2.3.0`.
- Update Compose Multiplatform to `1.10.0`.
- Circuits `BackHandler` uses now depend on `org.jetbrains.androidx.navigationevent`.

#### Code-gen

- Generate `@Origin` annotations for kotlin-inject-anvil and Metro code gen.
- Switch to `jakarta.inject` types for Dagger/Anvil code gen. This should have no source-breaking changes to users since this only affected generated code, but note that the square/anvil implementation may not support this in factory generation ([the KSP fork does](https://github.com/zacsweers/anvil)). If you need to only use javax annotations, use the `circuit.codegen.useJavaxOnly=true` KSP option.
- Drop KSP1 support.
- Fix not using named parameters with the `kotlin-inject` Anvil codegen mode.

### Contributors

Special thanks to the following contributors for contributing to this release!

- [@amirroid](https://github.com/amirroid)
- [@evanisnor](https://github.com/evanisnor)
- [@hossain-khan](https://github.com/hossain-khan)

0.31.0
------

_2025-11-04_

### Breaking Changes:

Remove `kotlinx-immutable` dependency. With Compose's [strong skipping mode](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping), we no longer feel this is necessary.

Result delivery is now handled by `NavigableCircuitContent` via an internal `AnsweringResultNavigator` wrapper, and result
handling APIs have been removed from `BackStack` and `SaveableBackStack` as this is now purely a navigation concern. This
separates result handling concerns from the backstack implementation and provides better encapsulation.

The callback block in `rememberAnsweringNavigator` is no longer suspendable as it was never intended for any long running operations.

### Behaviour Changes:

On iOS `GestureNavigationDecorationFactory` now uses `IOSPredictiveBackNavDecorator` instead of  `CupertinoGestureNavigationDecorator`.
This new decorator uses the Compose multiplatform `PredictiveBackHandler` to drive the back animation, instead of a swipeable gesture or nested scroll.

The `Navigator.resetRoot` API is now using a `StateOptions` class to allow saving, restoring, and
clearing of back stack state. The previous `resetRoot` function with `saveState` and `restoreState` booleans is available as an extension function.

### Misc:

- Switched Compose uses to depend on Compose Multiplatform over Jetpack Compose
- Android minimum SDK is now `minSdk` 23
- Added saved state peek/remove to `Backstack`
- Fixed `Navigator.onNavEvent()` not passing all arguments to `resetRoot()`
- Fixes to `rememberCircuitNavigator()` capturing `onRootPop` and not recreating if a new backstack was provided
- Update Compose Multiplatform to `1.9.2`.
- Update to Kotlin `2.2.21`.
- Update to Molecule `2.2.0`.
- Build against KSP `2.2.20-2.0.4`.

Special thanks to [@simon-duchastel](https://github.com/simon-duchastel) and [@eboudrant](https://github.com/eboudrant) for contributing to this release!

0.30.0
------

_2025-08-01_

### Behaviour Changes:

Rebuilt `continuityRetainedStateRegistry` as a common `lifecycleRetainedStateRegistry` and made `ViewModel` an implementation detail of it.

`AnimatedNavDecoration` is now using the full back stack to determine the transition animation.

### Updates to `ViewModelBackStackRecordLocalProvider`

`ViewModelBackStackRecordLocalProvider` has moved from the `backstack` module to the
`circuit-foundation` module while also changing to use the multiplatform `ViewModel` implementation.
`ViewModelBackStackRecordLocalProvider` is now provided as a default through the Circuit instance,
enabling customization of the default `BackStackRecordLocalProviders`.
Also added `backStackHostViewModel()` to access a `ViewModel` located in the `ViewModelStoreOwner` of `NavigableCircuitContent`.

### Misc:

- [gesture-navigation] Fix a crash when using `AndroidPredictiveBackNavDecorator` and having previously called `resetRoot()` with `restoreState=false`.
- [code gen] Added support for `CircuitContext` as an assisted param in code gen
- Update dagger to `v2.57`.
- Update Compose Android BOM to `2025.07.00`.
- Update androidx.lifecycle to `2.9.2`.

Special thanks to [@CamiloVega](https://github.com/CamiloVega) and [@kvaster](https://github.com/kvaster) for contributing to this release!


0.29.1
------

_2025-06-30_

- Fix `@AssistedFactory` code gen for Metro to use standard nested class semantics.

0.29.0
------

_2025-06-26_

- Update to Kotlin `2.2.0`.
- Update to KSP `2.2.0-2.0.2`.
- Support `@AssistedFactory` in code gen for Metro.
- Add seekable transition support to `AnimatedOverlay`.
- Add predictive back support to `FullScreenOverlay`.
- Compile against Android SDK 36.

0.28.1
------

_2025-06-09_

### Behaviour Changes:

Replacing `rememberContinuityCanRetainChecker()` with `CanRetainChecker.Always`, which changes the
Android `rememberRetained` behaviour to retain based on the `ViewModel` lifecycle. This should fix
`rememberRetained` not functioning as expected when used in a fragment. Previously `rememberRetained`
would only retain if the Activity was in a configuration change.

### Misc:

- Update to Kotlin `2.1.21`.
- Build against KSP `2.1.21-2.0.1`.
- Fix not being able to provide a custom ViewModel to `continuityRetainedStateRegistry()`
- Fix `rememberCircuitNavigator` and `rememberInterceptingNavigator` replaying a root pop at the same screen
- Update Compose Android BOM to `2025.06.00`.
- Update Compose Android to `1.8.2`.
- Update to Compose Multiplatform to `1.8.1`.
- Update androidx.lifecycle to `2.9.1`


0.28.0
------

_2025-05-12_

### **New**: circuitx-navigation adding navigation interception capabilities:

We've added a new navigation intercepting system to Circuit that lets you hook into and modify
navigation before it happens. The `InterceptingNavigator` sits before a regular Circuit `Navigator`,
giving you a chance to inspect or change navigation events. With the `NavigationInterceptor`
interface, you can handle `goTo`, `pop`, and `resetRoot` calls and decide if they should proceed,
fail, be skipped, or be rewritten to navigate somewhere else. There's also a`NavigationEventListener`
if you just want to know when navigation happens without changing it.
This new system is handy for advanced routing, blocking navigation, or tracking navigation events
for analytics.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-navigation:<version>")
}
```

Docs: https://slackhq.github.io/circuit/circuitx/navigation

### Misc:

- Fix the provided `Modifier` not being used in `NavigatorDefaults.EmptyDecoration`
- Fix `rememberCircuitNavigator()` not being able to propagate the root pop as its `BackHandler` was still active
- Added a common `rememberCircuitNavigator()` that uses a `BackHandler` to trigger `Navigator.pop` calls
- [docs] Add more alternative state designs.
- Update Compose Android BOM to `2025.05.00`.
- Update Compose Android to `1.8.1`.
- Update to Compose Multiplatform to `1.8.0`.
- Change the published jvmTarget to `11`
- Update molecule to `2.1.0`

0.27.1
------

_2025-04-14_

- **Fix**: Crash caused by calling `resetRoot` while the same `Screen` was still in the composition
- **New**: Added a sample app demonstrating bottom bar navigation
- [gesture-navigation] Update `AndroidPredictiveBackNavDecorator` to behave more like the Android predictive back motion spec
- [gesture-navigation] Change the Compose Material dependency to not be exposed as an api
- Update compose-bom to `2025.04.00`
- Update dagger to `2.56.1`
- Update kotlinInject.anvil to `0.1.3`
- Update kotlinx-coroutines to `1.10.2`
- Update androidx.core to `1.16.0`
- [samples] Update mosaic to `0.17.0`

Special thanks to [@OSemenovBoyarka](https://github.com/OSemenovBoyarka) and [@asapha](https://github.com/asapha) for contributing to this release!

0.27.0
------

_2025-02-28_

### Screen based animation overrides

We have added experimental support for animations based on the source/target screens and the type of navigation event. This can be accomplished with the the new `AnimatedScreenTransform` interface, allowing customization of the `ContentTransform` used when transitioning between screens. Having support for screen based animation overrides is especially important for use with shared element transitions, as it provides the ability to replace the transition before the shared elements are loaded.

See this [PR](https://github.com/slackhq/circuit/pull/1901) for more details and example implementations. Please share feedback in [this discussion](https://github.com/slackhq/circuit/discussions/1982).

### Behaviour Changes:

State retention logic was simplified by removing `LocalCanRetainChecker`, with `CanRetainChecker` becoming an implementation detail of a `RetainedStateRegistry`.
This potentially impacts uses of `rememberRetained` that depended on `LocalCanRetainChecker`, as `rememberRetained` would use the composition local before using `rememberCanRetainChecker`. Also, as `rememberCanRetainChecker` was only needed for `Continuity` it has been renamed to `rememberContinuityCanRetainChecker`.

### Misc:

- Fix a crash caused by calling `resetRoot` while `NavigableCircuitContent` was animating.
- Update androidx.activity to `v1.10.1`.
- [code gen] Make assisted injection detection more robust.
- [code gen] Update to KSP `2.1.10-1.0.31`.
- [code gen] Update to KotlinPoet `2.1.0`.
- [samples] Update mosaic to `0.16.0`
- [samples] Include shared element tutorial changes.

Special thanks to [@vulpeszerda](https://github.com/vulpeszerda) for contributing to this release!

0.26.1
------

_2025-02-13_

- Fix crash on right side back gesture.
- Update `CupertinoGestureNavigationDecoration` to be a `AnimatedNavDecorator`.
- Fix value restoration after `removeState` call in `RetainedStateHolder`.
- Update Android compose artifacts to `1.7.8`.
- [docs] Add tutorial for how to use Circuit shared elements.
- [docs] Added basic reference guide on deep-linking using circuit for Android platform.

Special thanks to [@vulpeszerda](https://github.com/vulpeszerda) for contributing to this release!

0.26.0
------

_2025-02-06_

Happy new year!

### Shared Elements API!

After a lot of iteration and work, this release adds support for Compose's new shared elements APIs.

These are still experimental and subject to change, both in Circuit and the underlying Compose APIs.

See this PR for full details as well as sample integrations: https://github.com/slackhq/circuit/pull/1550. Please share feedback in [this discussion](https://github.com/slackhq/circuit/discussions/1924). More formal docs to come as well, we'll publish updates there!

For now, the easiest way to support shared element transitions is to wrap your content with a `SharedElementTransitionLayout`.

```kotlin
CircuitCompositionLocals(circuit) {
  SharedElementTransitionLayout {
    NavigableCircuitContent(
      navigator = navigator,
      backStack = backStack,
    )
  }
}
```

`SharedElementTransitionLayout` creates and provides a `SharedElementTransitionScope` to content within it, and in turn exposes a `SharedTransitionScope` for use with standard compose shared elements/bounds animations. This is supported in `NavigableCircuitContent` and overlays.

There is also a `PreviewSharedElementTransitionLayout` for help with Compose previews.

### Behaviour Changes: `rememberRetained`

Previously, `rememberRetained` could sometimes restore values when a composable was re-added, depending on whether its parent `RetainedStateRegistry` had been saved (#1783).
Now, `rememberRetained` aligns with `remember` and `rememberSaveable`: if a composable is removed and later re-added, its value will not be restored unless it is explicitly saved and then restored via the registry.

Update rememberRetained to allow CanRetainChecker to be updated in place.

### Behaviour Change: `RetainedStateRegistry`

- `saveAll` now returns the saved values.
- `RetainedStateRegistry.Entry.unregister` now returns whether the unsaved valueProvider was actually removed.
- `saveAll` and `saveValue` now skip storing child values when `CanRetainChecker` returns `false`.

### New: `RetainedStateHolder`

Similar to `SaveableStateHolder`, `RetainedStateHolder` provides a mechanism to maintain separate `RetainedStateRegistry` entries for specific keys. This allows saving the state defined with `rememberRetained` for a subtree before it is disposed, so that the subtree can later be recomposed with its state restored.

```kotlin
val retainedStateHolder = rememberRetainedStateHolder()
var currentTab by remember { mutableStateOf(TabA) }

retainedStateHolder.RetainedStateProvider(key = currentTab.name) {
  // rememberRetained values in tab content are preserved across tab switches
  when (currentTab) {
    TabA -> {
      TabAContent()
    }
    TabB -> {
      TabBContent()
    }
    TabC -> {
      TabCContent()
    }
  }
}
```

### Implementation Changes: `NavigableCircuitContent`

- The approach of managing a separate `RetainedStateRegistry` for each record has been changed to use `RetainedStateHolder` instead.
- Change `SaveableStateHolder` to release saved states of removed records.

### Misc

- Fixe an issue causing codegen to fail for class @Inject annotations.
- Compile against Android SDK 35.
- Update Compose Android BOM to `2025.01.01`.
- Update to androidx.annotation `1.9.1`.
- Update to androidx.activity `1.10.0`.
- Update to Compose Android `1.7.7`.
- Update to Compose Multiplatform `1.7.3`.
- Update to Kotlin `1.9.10`.
- [code gen] Update to KSP `1.9.10-1.0.29`.
- [code gen] Update to Dagger `2.55`.
- [code gen] Update to KotlinPoet `2.0.0`.
- [code gen] Build against Anvil-KSP `0.4.1`. Should still be compatible with square/anvil as well.
- [code gen] Build against kotlin-inject-anvil `0.1.2`. Should still be compatible with square/anvil as well.
- [samples] Update mosaic + modernize mosaic counter sample to fully use effects.
- [docs] Fix variable casing in Navigation documentation example.

Special thanks to [@vulpeszerda](https://github.com/vulpeszerda), [@rharter](https://github.com/rharter), [@alexvanyo](https://github.com/alexvanyo), and [@easyhooon](https://github.com/easyhooon) for contributing to this release!

0.25.0
------

_2024-10-23_

- **New**: Pass `tonalElevation` to `BottomSheetOverlay` so that is reflected in `ModalBottomSheet`.
- **Enhancement**: Add `-dontwarn` on Anvil classes from codegen annotations.
- **Enhancement**: Misc small doc fixes and updates.
- **Enhancement**: Add note about only creating a presenter for supported screen types in tutorial.
- Update kotlinInject.anvil to `0.0.5`.
- Update androidx.activity to `1.9.3`.
- Update androidx.lifecycle to `2.8.6`.
- Update Kotlin to `2.0.21`.
- Build against KSP to `2.0.21-1.0.25`.
- Update Turbine to `1.2.0`.
- Update Compose Android to `1.7.4`.
- Update Compose Multiplatform to `1.7.0`.

Special thanks to [@eboudrant](https://github.com/eboudrant), [@bidrohi](https://github.com/bidrohi), [@hossain-khan](https://github.com/hossain-khan), and [@dturner](https://github.com/dturner) for contributing to this release!

0.24.0
------

_2024-09-16_

- **New**: Add code gen support for [kotlin-inject](https://github.com/evant/kotlin-inject) + [kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil). See the [code gen docs](https://slackhq.github.io/circuit/code-gen/) for usage instructions. We've also added a sample project.
- **New**: `presenterTestOf()` and `Presenter.test()` functions now return a new `CircuitReceiveTurbine` interface. This interface slightly but importantly modifies the behavior of `awaitItem()` by making it only emit _changed_ items rather than every item. If you do want to assert the equivalent state is emitted across recompositions, you can use `awaitUnchanged()`.
- **New**: Promote `LocalBackStack` to public API.
- **Behavior change**: Due to the above-documented change to `awaitItem()`, you may need to update tests that previously assumed duplicate emissions.
- Update to Kotlin `2.0.20`.
- **Change**: Switch to stdlib's implementation of `Uuid`. This release now requires Kotlin `2.0.20` or later.
- Add sample test to demonstrate `rememberAnsweringNavigator` result handling.
- Update to kotlinx.coroutines `1.9.0`.
- Update to compose-bom `2024.09.01`.
- Update to kotlinx.collections.immutable `0.3.8`.
- Update to androidx.activity `1.9.2`.
- Update to androidx.lifecycle `2.8.5`.
- Update to kotlin `2.0.20`.
- Build against KSP `2.0.20-1.0.25`.

0.23.1
------

_2024-07-30_

- **New**: Promote `NoOpRetainedStateRegistry` to public API for use in testing and previews.
- **New**: Add `CircuitPreview` helper function for composable previews that contain Circuit content.
- **Enhancement**: When running under `LocalInspectionMode`, Circuit's default `onUnavailableContent` now shows a simpler non-intrusive placeholder UI instead.
- **Enhancement**: Support secondary injected constructors in code gen.
- **Fix**: Fix non-dismissable `BottomSheetOverlay` crash when invoking back-press.
- Update to Kotlin `2.0.10`.
- Update to androidx.annotation `1.8.2`.
- Build against KSP `2.0.10-1.0.24`.

0.23.0
------

_2024-07-30_

- **New**: Add macOS, windows, linux, tvOS, and watchOS targets to Circuit's runtime and retained artifacts.
- **New**: Add macOS targets to Circuit's UI, backstack, overlay, test, circuitx (except gesture nav), and foundation artifacts.
- Slightly optimize `pausableState` [#1455](https://github.com/slackhq/circuit/pull/1455).
- **Change**: `OverlayHost.showFullScreenOverlay` now returns the `PopResult?` that was popped by the screen.
- **Change**: Remove `backstack` dependency from `circuit-runtime`. It was unnecessary and also accidentally imposed Compose UI on the runtime/presenter artifacts.
- **Change**: Make `Overlay` a `fun interface`.
- **Breaking Change**: Change `OverlayEffect` to use a new `OverlayScope` API that implements both `CoroutineScope` and `OverlayHost`, making both contexts available in the lambda.
- Update KSP to `2.0.0-1.0.24`.
- Update androidx.activity to `1.9.1`.
- Update androidx.lifecycle to `2.8.4`.
- Update androidx.annotation to `1.8.1`.
- Update KotlinPoet `1.18.1`.
- Update Compose Multiplatform to `1.6.11`.

Special thanks to [@aschulz90](https://github.com/aschulz90) and [@chrisbanes](https://github.com/chrisbanes) for contributing to this release!

0.22.2
------

_2024-06-04_

- **Fix**: Fix `pausableState` recomposition loops by avoiding backward snapshot writes.
- **New**: Add `Circuit.presentWithLifecycle` flag to enable/disable automatic `pausableState` use. This is enabled by default.
- Update Compose Multiplatform to `1.6.11`.
- Update androidx.lifecycle to `2.8.1`.
- Update androidx.appcompat to `1.7.0`.

Special thanks to [@chrisbanes](https://github.com/chrisbanes), [@adamp](https://github.com/adamp), and Chuck Jazdzewski for contributing to this release and helping us find a runtime fix for the `pausableState` issue!

0.22.1
------

_2024-05-28_

- **Fix**: Fix `rememberRetained` implicitly requiring `LocalContext` where it used to no-op.
- Update Molecule to `2.0.0`.

0.22.0
------

_2024-05-28_

- Update to Kotlin `2.0.0`.
- Update to KSP `2.0.0-1.0.21`.
- Update Compose Multiplatform to `1.6.10`.
- Switch to the compose compiler shipped with Kotlin.

This release is otherwise identical to `0.21.0`, just updated to Kotlin 2.0.

0.21.0
------

_2024-05-28_

- **New**: Add WASM targets.
- **New**: Add `FakeNavigator` functions to check for the lack of pop/resetRoot events.
- **New**: Add `FakeNavigator` constructor param to add additional screens to the backstack.
- **New**: Add support for static UIs. In some cases, a UI may not need a presenter to compute or manage its state. Examples of this include UIs that are stateless or can derive their state from a single static input or an input [Screen]'s properties. In these cases, make your _screen_ implement the `StaticScreen` interface. When a `StaticScreen` is used, Circuit will internally allow the UI to run on its own and won't connect it to a presenter if no presenter is provided.
- **New**: Add `RecordLifecycle` and `LocalRecordLifecycle` composition local, allowing UIs and presenters to observe when they are 'active'. Currently, a record is considered 'active' when it is the top record on the back stack.
- **New**: Add a `rememberRetainedSaveable` variant that participates in both `RetainedStateRegistry` and `SaveableStateRegistry` restoration, allowing layered state persistence.
  - The logic is the following upon `rememberRetainedSaveable` entering composition:
    - consume from both `RetainedStateRegistry` and `SaveableStateRegistry`, if available
    - if the retained value is available, use that
    - otherwise, if the saveable restored value is available, use that
    - otherwise, re-initialize the value
  - There is also an overload of `rememberRetained` that explicitly requires a `Saver` parameter.
- **Behaviour Change**: Presenters are now 'paused' when inactive and replay their last emitted `CircuitUiState` when they are not active. Presenters can opt-out of this behavior by implementing `NonPausablePresenter`.
- **Behaviour Change**: `NavigatorImpl.goTo` no longer navigates if the `Screen` is equal to `Navigator.peek()`.
- **Behaviour Change**: `Presenter.present` is now annotated with `@ComposableTarget("presenter")`. This helps prevent use of Compose UI in the presentation logic as the compiler will emit a warning if you do. Note this does not appear in the IDE, so it's recommended to use `allWarningsAsErrors` to fail the build on this event.
- **Behaviour Change**: Guard against `Navigator.goTo()` calls to the same current screen.
- **Change**: `Navigator.goTo` now returns a Bool indicating navigation success.
- **Change**: Move iOS `GestureNavigationDecoration` impl to `commonMain` and rename to `CupertinoGestureNavigationDecoration`.
- **Change**: Target jvmTarget `1.8` in core libraries.
- **Fix**: Fix saveable state being restored when using reset root navigation events.
- **Deprecation**: `FakeNavigator.assertIsEmpty` and `expectNoEvents` (use the specific event type methods instead)
- Mark `Presenter.Factory` as `@Stable`.
- Mark `Ui.Factory` as `@Stable`.
- Mark `CircuitContext` as `@Stable`.
- Mark `EventListener` as `@Stable`.
- Mark `EventListener.Factory` as `@Stable`.
- [samples] Improve interop sample significantly.
- Update Kotlin to `1.9.24`.
- Update KSP to `1.9.24-2.0.20`.
- Update compose-compiler to `1.5.14`.
- Update KotlinPoet to `1.17.0`.
- Update androidx.lifecycle to `2.8.0`.
- Update Molecule to `1.4.3`.
- Update androidx.annotation to `1.8.0`.
- Update kotlinx.coroutines to `1.8.1`.
- Update Compose Multiplatform to `1.6.2`.
- Update Compose UI to `1.6.7`.
- Update Compose Runtime to `1.6.7`.
- Update Compose Animation to `1.6.7`.
- Update Compose Material to `1.6.7`.
- Update androidx.core to `1.13.1`.
- Update androidx.activity to `1.9.0`.
- Update dagger to `2.51.1`.
- Update uuid to `0.8.4`.

Special thanks to [@chrisbanes](https://github.com/chrisbanes), [@alexvanyo](https://github.com/alexvanyo), [@eboudrant](https://github.com/eboudrant), [@edenman](https://github.com/edenman), and [@JustinBis](https://github.com/JustinBis) for contributing to this release!

0.20.0
------

_2024-03-18_

- **New**: Enable `RememberObserver` to work with `rememberRetained`.
- **New**: Add `Navigator.popRoot()`. extension (#1274)
- **Behavior change**: Add a key to `CircuitContent` to keep `Ui` and `Presenter` consistent. We already did this for presenters, this just makes it consistent for both.
- [circuitx-android] Implement `ToastEffect`.
- **Fix**: Fix `rememberImpressionNavigator()` not delegating `PopResult`.
- **Fix**: Navigator - Pass `PopResult` to `onRootPop()`.
- **Fix**: Check `canRetainCheck` when saving `RetainedStateRegistry`.
- **Enhancement**: Improve error messaging when using assisted inject.
- Force `com.google.guava:listenablefuture` to `1.0` to avoid conflicts with Guava.
- Update compose-compiler to `1.5.10.1`.
- Update coroutines to `1.8.0`.
- Update to Compose Multiplatform `1.6.1`.
- Update Android compose dependencies to `1.6.3`.
- Update molecule to `1.4.1`.
- Update dagger to `2.51`.
- Update turbine to `1.1.0`.
- Update uuid to `0.8.3`.
- Update kotlin to `1.9.23`.
- Update KSP to `1.9.23-1.0.19`.

Special thanks to [@chrisbanes](https://github.com/chrisbanes), [@aschulz90](https://github.com/aschulz90), and [@alexvanyo](https://github.com/alexvanyo) for contributing to this release!

0.19.1
------

_2024-02-12_

This is a small bug fix release focused `SaveableBackStack` consistency and `FakeNavigator` API improvements.

- Fix `FakeNavigator.awaitNextScreen()` not suspending.
- Fix `FakeNavigator.resetRoot()` not returning the actual popped screens.
- Make `Navigator.peekBackStack()` and `Navigator.resetRoot()` return `ImmutableList`.
- Make `BackStack.popUntil()` return the `ImmutableList` of the popped records.
- Support `FakeNavigator.peekBackStack()` return the `ImmutableList` of the popped records.
- Strongly pop events and resetRoot events in `FakeNavigator`. This should offer much more information about the events.
- Use a real `BackStack` instance in `FakeNavigator` + allow for specifying a user-provided instance.
- Require an initial root screen to construct `FakeNavigator` unless using a custom `BackStack`.
  - Note this slightly changes semantics, as now the root screen will not be recorded as the first `goTo` event.
- Require an initial root screen (or list of screens) for `rememberSaveableBackStack()`.
- Expose a top-level non-composable `Navigator()` factory function.

0.19.0
------

_2024-02-09_

### Navigation with results

This release introduces support for inter-screen navigation results. This is useful for scenarios where you want to pass data back to the previous screen after a navigation event, such as when a user selects an item from a list and you want to pass the selected item back to the previous screen.

```kotlin
var photoUrl by remember { mutableStateOf<String?>(null) }
val takePhotoNavigator = rememberAnsweringNavigator<TakePhotoScreen.Result>(navigator) { result ->
  photoUrl = result.url
}

// Elsewhere
takePhotoNavigator.goTo(TakePhotoScreen)

// In TakePhotoScreen.kt
data object TakePhotoScreen : Screen {
  @Parcelize
  data class Result(val url: String) : PopResult
}

class TakePhotoPresenter {
  @Composable fun present(): State {
    // ...
    navigator.pop(result = TakePhotoScreen.Result(newFilters))
  }
}
```

See the [new section in the navigation docs](https://slackhq.github.io/circuit/navigation/#results) for more details, as well as [updates to the Overlays](https://slackhq.github.io/circuit/overlays/#overlay-vs-popresult) docs that help explain when to use an `Overlay` vs navigating to a `Screen` with a result.

### Support for multiple back stacks

This release introduces support for saving/restoring navigation state on root resets (aka multi back stack). This is useful for scenarios where you want to reset the back stack to a new root but still want to retain the previous back stack's state, such as an app UI that has a persistent bottom navigation bar with different back stacks for each tab.

This works by adding two new optional `saveState` and `restoreState` parameters to `Navigator.resetRoot()`.

```kotlin
navigator.resetRoot(HomeNavTab1, saveState = true, restoreState = true)
// User navigates to a details screen
navigator.push(EntityDetails(id = foo))
// Later, user clicks on a bottom navigation item
navigator.resetRoot(HomeNavTab2, saveState = true, restoreState = true)
// Later, user switches back to the first navigation item
navigator.resetRoot(HomeNavTab1, saveState = true, restoreState = true)
// The existing back stack is restored, and EntityDetails(id = foo) will be top of
// the back stack
```

There are times when saving and restoring the back stack may not be appropriate, so use this feature only when it makes sense. A common example where it probably does not make sense is launching screens which define a UX flow which has a defined completion, such as onboarding.

### New Tutorial!

On top of Circuit's existing docs, we've added a new tutorial to help you get started with Circuit. It's a step-by-step guide that walks you through building a simple inbox app using Circuit, intended to serve as a sort of small code lab that one could do in 1-2 hours. Check it out [here](https://slackhq.github.io/circuit/tutorial/).

### Overlay Improvements

- **New**: Promote `AlertDialogOverlay`, `BasicAlertDialogOverlay`, and `BasicDialogOverlay` to `circuitx-overlay`.
- **New**: Add `OverlayEffect` to `circuit-overlay`. This offers a simple composable effect to show an overlay and await a result.
  ```kotlin
  OverlayEffect(state) { host ->
    val result = host.show(AlertDialogOverlay(...))
    // Do something with the result
  }
  ```
- Add `OverlayState` and `LocalOverlayState` to `circuit-overlay`. This allows you to check the current overlay state (`UNAVAILABLE`, `HIDDEN`, or `SHOWING`).
- Mark `OverlayHost` as `@ReadOnlyOverlayApi` to indicate that it's not intended for direct implementation by consumers.
- Mark `Overlay` as `@Stable`.

### Misc

- Make `NavEvent.screen` public.
- Change `Navigator.popUntil` to be exclusive.
- Add `Navigator.peek()` to peek the top screen of the back stack.
- Add `Navigator.peekBackStack()` to peek the top screen of the back stack.
- Align spelling of back stack parameters across all APIs to `backStack`.
- Refreshed iOS Counter sample using SPM and SKIE.
- Convert STAR sample to KMP. Starting with Android and Desktop.
- Fix baseline profiles packaging. Due to a bug in the baseline profile plugin, we were not packaging the baseline profiles in the artifacts. This is now fixed.
- Mark `BackStack.Record` as `@Stable`.
- Fix an infinite loop in the `onRootPop` of the Android `rememberCircuitNavigator`.
- Update the default decoration to better match the android 34 transitions.
- Update androidx.lifecycle to `2.7.0`.
- Update to compose multiplatform to `1.5.12`.
- Update to compose to `1.6.1`.
- Update to compose-bom to `2024.02.00`.
- Update compose-compiler to `1.5.9`.
- Update AtomicFu to `0.23.2`.
- Update Anvil to `2.4.9`.
- Update KotlinPoet to `1.16.0`.
- Compile against KSP `1.9.22-1.0.17`.

Special thanks to [@milis92](https://github.com/milis92), [@ChrisBanes](https://github.com/ChrisBanes), and [@vulpeszerda](https://github.com/vulpeszerda) for contributing to this release!

0.18.2
------

_2024-01-05_

- **Fix**: Fix lifetime of `Record`s' `ViewModelStores`. This fully fixes [#1065](https://github.com/slackhq/circuit/issues/1065).
- Update Molecule to `1.3.2`.
- Update Jetbrains' compose-compiler to `1.5.7.1`.

Special thanks to [@dandc87](https://github.com/dandc87) for contributing to this release!

0.18.1
------

_2024-01-01_

- **Fix**: Fix popped Record's `ProvidedValues` lifetime. See [#1065](https://github.com/slackhq/circuit/issues/1065) for more details.
- **Fix**: Fix `GestureNavDecoration` dropping saveable/retained state on back gestures. See [#1089](https://github.com/slackhq/circuit/pull/1089) for more details.

Special thanks to [@ChrisBanes](https://github.com/chrisbanes) and [@dandc87](https://github.com/dandc87) for contributing to this release!

0.18.0
------

_2023-12-29_

- **New**: Support animating an overlay out after returning a result with `AnimatedOverlay`.
- **Fix**: Fix dropping back stack retained state on Android Activity rotations.
- **Enhancement**: Add ability to customize `ModalBottomSheet` appearance in `BottomSheetOverlay`.
- Update Kotlin to `1.9.22`.
- Update KSP to `1.9.22-1.0.16`.
- Update Dagger to `2.50`.
- Update kotlinx-collections-immutable to `0.3.7`.
- Update AndroidX Activity to `1.8.2`.

Special thanks to [@ChrisBanes](https://github.com/ChrisBanes), [@chriswiesner](https://github.com/chriswiesner), and [@BryanStern](https://github.com/BryanStern) for contributing to this release!

0.17.1
------

_2023-12-05_

- **Enhancement**: Commonize `SaveableStateRegistryBackStackRecordLocalProvider` to be supported across all currently supported platforms.
- **Fix**: Fix `LocalBackStackRecordLocalProviders` always returning a new composition local.
- Update `androidx.compose.compiler:compiler` to `1.5.5`
- Update KotlinPoet to `1.15.3`
- Update Dagger to `2.49`

Special thanks to [@alexvanyo](https://github.com/alexvanyo) for contributing to this release.

0.17.0
------

_2023-11-28_

### **New**: circuitx-effects artifact

The circuitx-effects artifact provides some effects for use with logging/analytics. These effects
are typically used in Circuit presenters for tracking `impressions` and will run only once until
forgotten based on the current circuit-retained strategy.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-effects:<version>")
}
```

Docs: https://slackhq.github.io/circuit/circuitx/#effects

### **New**: Add codegen mode to support both Anvil and Hilt

Circuit's code gen artifact now supports generating for Hilt projects. See the docs for usage instructions: https://slackhq.github.io/circuit/code-gen/

### Misc

- Decompose various `CircuitContent` internals like `rememberPresenter()`, `rememberUi`, etc for reuse.
- Make `CircuitContent()` overload that accepts a pre-constructed presenter/ui parameters public to allow for more control over content.
- [samples] Update README to include the interop sample.
- [samples] Various bugfixes to samples.
- [docs] Link sources in kdocs.
- [docs] Nest CircuitX artifacts in kdocs ToC.
- Update uuid to `0.8.2`.
- Update KotlinPoet to `1.15.1`.
- Update to Compose Multiplatform `1.5.11`.
- Update to Kotlin `1.9.21`.
- Update to KSP `1.9.21-1.0.15`.
- Update to compose-compiler (multiplatform) `1.5.4`.
- Update to Molecule `1.3.1`.

Special thanks to [@jamiesanson](https://github.com/jamiesanson), [@frett](https://github.com/frett), and [@bryanstern](https://github.com/BryanStern) for contributing to this release!

0.16.1
------

_2023-11-09_

- **Fix**: Fix retained value not recalculating if inputs changed.
- Build against KSP `1.9.20-1.0.14`.

0.16.0
------

_2023-11-01_

- **New**: `circut-retained` is now enabled automatically in `CircuitCompositionLocals` by default, we still allowing overriding it with no-op implementation.
- Update to Kotlin `1.9.20`.
- Update Compose Multiplatform to `1.5.2`.
- Update `agp` to `8.1.2`.
- Update `androidx.activity` to `1.8.0`.
- Update `benchmark` to `1.2.0`.
- Update `coil` to `2.5.0`.
- Update `compose.material3` to `1.1.2`.
- Update `compose.material` to `1.5.4`.
- Update `compose.runtime` to `1.5.4`.
- Update `compose.ui` to `1.5.4`.
- Update `roborazzi` to `1.6.0`.


0.15.0
------

_2023-09-20_

### **New**: Allow retained state to be retained whilst UIs and Presenters are on the back stack.

Originally, `circuit-retained` was implemented as a solution for preserving arbitrary data across configuration changes on Android. With this change it now also acts as a solution for retaining state _across the back stack_, meaning that traversing the backstack no longer causes restored contents to re-run through their empty states anymore.

To support this, each back stack entry now has its own `RetainedStateRegistry` instance.

Note that `circuit-retained` is still optional for now, but we are considering making it part of `CircuitCompositionLocals` in the future. Please let us know your thoughts in this issue: https://github.com/slackhq/circuit/issues/891.

Full details + demos can be found in https://github.com/slackhq/circuit/pull/888. Big thank you to [@chrisbanes](https://github.com/chrisbanes) for the implementation!

### Other changes

- **New**: Add `collectAsRetainedState` utility function, analogous to `collectAsState` but will retain the previous value across configuration changes and back stack entries.
- **Enhancement**: Optimize `rememberRetained` with a port of the analogous optimization in `rememberSaveable`. See [#850](https://github.com/slackhq/circuit/pull/850).
- **Enhancement**: `Presenter` and `Ui` interfaces are now annotated as `@Stable`.
- **Fix**: Fix `GestureNavigationDecoration` function parameter order.
- **Fix**: `BackHandler` on iOS now has the proper file name.
- **Fix**: Key the `presenter.present()` in `CircuitContent` on the `Screen` rather than the `presenter` itself, which fixes a severe issue that prevented `currentCompositeKeyHash` from working correctly on `rememberRetained` and `rememberSaveable` uses.
- Update CM compiler to `1.5.2`.
- Update CM to `1.5.1`.
- Update `androidx.compose.animation` to `1.5.1`.
- Update `androidx.compose.foundation` to `1.5.1`.
- Update `androidx.compose.runtime` to `1.5.1`.
- Update `androidx.compose.material` to `1.5.1`.
- Update `androidx.lifecycle` to `2.6.2`.
- Update `androidx.annotation` to `1.7.0`.

0.14.1
------

_2023-09-03_

- **New**: Add `GestureNavigationDecoration` to `CircuitX` courtesy of [@chrisbanes](https://github.com/chrisbanes).

This is a new `NavDecoration` that allows for gesture-based navigation, such as predictive back in Android 14 or drag gestures in iOS. See the [docs](https://slackhq.github.io/circuit/circuitx/#gesture-navigation) for more details.

```kotlin
NavigableCircuitContent(
  navigator = navigator,
  backstack = backstack,
  decoration = GestureNavigationDecoration(
    // Pop the back stack once the user has gone 'back'
    navigator::pop
  )
)
```

- Fix embedded baseline profiles in published artifacts. Unfortunately GMDs used to generate these are quite finicky to run so these are sometimes tricky to regen each release.

Special thanks to [@chrisbanes](https://github.com/chrisbanes) and [@alexvanyo](https://github.com/alexvanyo) for contributing to this release!

0.14.0
------

_2023-08-30_

- **New**: Circuit now supports JS targets!
- **New**: Introduce CircuitX artifacts. CircuitX is a suite of extension artifacts for Circuit. These artifacts are intended to be
  batteries-included implementations of common use cases, such as out-of-the-box `Overlay` types or
  Android navigation interop. See the [docs](https://slackhq.github.io/circuit/circuitx/) for more details.
- **Enhancement**: Promote `Screen` to its own artifact. This is now under the `com.slack.circuit.runtime.screen.Screen` name.
- **Enhancement**: Use `Screen` directly in the `BackStack` in place of `route`.
- **Enhancement**: No longer require `SaveableBackStack` in `NavigableCircuitContent`, now any `BackStack` impl is supported.
- **Enhancement**: Make `CanRetainChecker` more customizable in `circuit-retained`.
- **Enhancement**: Pass the whole list of active records to `DecoratedContent`, allowing more complex handling of back gestures (predictive back in android, drag gestures in iOS, etc).
- **Enhancement**: Refactor out a `buildCircuitContentProviders()` in `NavigableCircuitContent`, which enables `movableContentOf` to work since it's reusing the same instance for records across changes.
- **Fix**: Fix duplicated `Modifier` for `DecoratedContent`.
- **Fix**: Fix new presenter instances of the same type not being recomposed. See [#799](https://github.com/slackhq/circuit/pull/799) for more details.
- **Fix**: Export iOS targets for `circuit-test` artifact.
- Demonstrate back handling for Compose Multiplatform in Counter sample.
- Add `kotlinx.collections.immutable` to core APIs.
- Update to Compile SDK 34.
- Update to Compose Multiplatform `1.5.0`.
- Update androidx.compose.compiler to `1.5.3`.
- Update androidx.compose.ui to `1.5.0`.
- Update androidx.compose.material to `1.5.0`.
- Update androidx.compose.runtime to `1.5.0`.
- Update androidx.compose.foundation to `1.5.0`.
- Update uuid to `0.8.1`.
- Update Molecule to `1.2.0`.
- Update Kotlin to `1.9.10`.
- Update KSP to `1.9.10-1.0.13`.

Thanks to [@chrisbanes](https://github.com/chrisbanes) and [@ashdavies](https://github.com/ashdavies) for contributing to this release!

0.13.0-beta01
-------------

_2023-08-17_

- **New**: Circuit now supports JS targets!
- **New**: Introduce CircuitX artifacts. CircuitX is a suite of extension artifacts for Circuit. These artifacts are intended to be
  batteries-included implementations of common use cases, such as out-of-the-box `Overlay` types or
  Android navigation interop. See the [docs](https://slackhq.github.io/circuit/circuitx/) for more details.
- **Fix**: Fix new presenter instances of the same type not being recomposed. See [#799](https://github.com/slackhq/circuit/pull/799) for more details.
- **Fix**: Export iOS targets for `circuit-test` artifact.
- Update to Compile SDK 34.
- Update to Compose Multiplatform `1.5.0-beta02`.
- Update androidx.compose.ui to `1.5.0`.
- Update androidx.compose.material to `1.5.0`.
- Update androidx.compose.runtime to `1.5.0`.
- Update androidx.compose.foundation to `1.5.0`.
- Update Molecule to `1.2.0`.
- Update KSP to `1.9.0-1.0.13`.

Note this release is a beta release due to the dependency on CM `1.5.0-beta02`.

0.12.1
------

_2023-08-01_

- Patch release with baseline profiles bundled in the Android artifacts again.
- Update to Anvil `2.4.7`.

0.12.0
------

_2023-07-28_

- [foundation] Rename `CircuitConfig` -> `Circuit`. There is a source-compatible typealias for `CircuitConfig` left with a deprecation replacement to ease migration.
- [foundation] Rename `CircuitContext.config` -> `CircuitContext.circuit`. The previous `CircuitContext.config` function is left with a deprecation replacement to ease migration.
- [test] Add new `TestEventSink` helper for testing event emissions in UI tests.
- [overlay] Add missing coroutines implementation dependency.
- Update to Kotlin `1.9.0`.
- Update to KSP `1.9.0-1.0.12`.
- Update to Compose Multiplatform `1.4.3`.
- Update to Coroutines `1.7.3`.
- Update to Compose compiler to `1.5.1` (androidx) and `1.5.0` (compose-multiplatform).
- Update [uuid](https://github.com/benasher44/uuid) to `0.8.0`.

0.11.0
------

_2023-07-20_

- [runtime] Fix race condition in `EventListener.start()` callback.
- [code gen] Update to Dagger 2.47.
- [docs] No longer recommend or require extracting intermediate event sink variables. This is no longer an issue 🎉.
- Update Molecule to `1.0.0`.

Thanks to [@bryanstern](https://github.com/bryanstern) for contributing to this release!

0.10.1
------

_2023-07-09_

- [runtime] Make `CircuitContent` overload with `Navigator` public.
- [runtime] Remember `Presenter` and `Ui` in `CircuitContent`.
- [runtime] Fix kdoc typo in `RememberRetained` .

Special thanks to [@chrisbanes](https://github.com/chrisbanes) and [@bryanstern](https://github.com/bryanstern) for contributing to this release!

0.10.0
------

_2023-06-30_

- [runtime] Fix wrong compose-compiler used in iOS targets. Now we're using the compose-multiplatform fork.
- [runtime] Allow creation of multiple `RetainedStateRegistry` instances.
- [docs] Add clarifying links to Events docs.
- [samples] Add new image detail view in STAR sample.
- Update Molecule to `0.11.0`.
- Update AndroidX compose-compiler to `1.4.8`.
- Update compose-multiplatform to `1.4.1`.
- Update to coroutines `1.7.2`.
- Update to Turbine `1.0.0`.
- Update to Kotlin `1.8.22`.

Special thanks to [@bryanstern](https://github.com/bryanstern), [@saket](https://github.com/saket), and [@chrisbanes](https://github.com/chrisbanes) for contributing to this release!

0.9.1
-----

_2023-06-02_

- [runtime] Promote `NavEvent` subtypes to public API.
- [runtime] Update `com.benasher44:uuid` to `0.7.1`.
- [code gen] Update Anvil to `2.4.6`.

0.9.0
-----

_2023-05-26_

### Preliminary support for iOS targets

Following the announcement of Compose for iOS alpha, this release adds `ios()` and `iosSimulatorArm64()` targets for the Circuit core artifacts. Note that this support doesn't come with any extra APIs yet for iOS, just basic target support only. We're not super sure what direction we want to take with iOS, but encourage others to try it out and let us know what patterns you like. We have updated the Counter sample to include an iOS app target as well, using Circuit for the presentation layer only and SwiftUI for the UI.

Note that circuit-codegen and circuit-codegen-annotations don't support these yet, as Anvil and Dagger only support JVM targets.

More details can be found in the PR: https://github.com/slackhq/circuit/pull/583

### Misc

- Use new baseline profile plugin for generating baseline profiles.
- Misc sample app fixes and updates.
- Add window size class example to STAR sample.
- Switch to Roborazzi for screenshot test samples.
- Small documentation updates.
- Add bi-directional Flow/Circuit interop to interop sample.

Note that we unintentionally used an experimental animation API for `NavigatorDefaults.DefaultDecotration`, which may cause R8 issues if you use a newer, experimental version of Compose animation. To avoid issues, copy the animation code and use your own copy compiled against the newest animation APIs. We'll fix this after Compose 1.5.0 is released.

### Dependency updates

```
androidx.activity -> 1.7.2
compose -> 1.4.3
compose-compiler -> 1.4.7
coroutines -> 1.7.1
kotlin -> 1.8.21
kotlinpoet -> 1.13.2
turbine -> 0.13.0
```

0.8.0
-----

_2023-04-06_

### [Core] Split up core artifacts.
- `circuit-runtime`: common runtime components like `Screen`, `Navigator`, etc.
- `circuit-runtime-presenter`: the `Presenter` API, depends on `circuit-runtime`.
- `circuit-runtime-ui`: the `Ui` API, depends on `circuit-runtime`.
- `circuit-foundation`: the circuit foundational APIs like `CircuitConfig`, `CircuitContent`, etc. Depends on the first three.

The goal in this is to allow more granular dependencies and easier building against subsets of the API. For example, this would allow a presenter implementation to easily live in a standalone module that doesn't depend on any UI dependencies. Vice versa for UI implementations.

Where we think this could really shine is in multiplatform projects where Circuit's UI APIs may be more or less abstracted away in service of using native UI, like in iOS.

#### `circuit-runtime` artifact
| Before                           | After                                    |
|----------------------------------|------------------------------------------|
| com.slack.circuit.CircuitContext | com.slack.circuit.runtime.CircuitContext |
| com.slack.circuit.CircuitUiState | com.slack.circuit.runtime.CircuitUiState |
| com.slack.circuit.CircuitUiEvent | com.slack.circuit.runtime.CircuitUiEvent |
| com.slack.circuit.Navigator      | com.slack.circuit.runtime.Navigator      |
| com.slack.circuit.Screen         | com.slack.circuit.runtime.Screen         |

#### `circuit-runtime-presenter` artifact
| Before                      | After                                         |
|-----------------------------|-----------------------------------------------|
| com.slack.circuit.Presenter | com.slack.circuit.runtime.presenter.Presenter |

#### `circuit-runtime-ui` artifact
| Before               | After                                  |
|----------------------|----------------------------------------|
| com.slack.circuit.Ui | com.slack.circuit.runtime.presenter.Ui |

#### `circuit-foundation` artifact
| Before                                     | After                                                 |
|--------------------------------------------|-------------------------------------------------------|
| com.slack.circuit.CircuitCompositionLocals | com.slack.circuit.foundation.CircuitCompositionLocals |
| com.slack.circuit.CircuitConfig            | com.slack.circuit.foundation.CircuitConfig            |
| com.slack.circuit.CircuitContent           | com.slack.circuit.foundation.CircuitContent           |
| com.slack.circuit.EventListener            | com.slack.circuit.foundation.EventListener            |
| com.slack.circuit.NavEvent                 | com.slack.circuit.foundation.NavEvent                 |
| com.slack.circuit.onNavEvent               | com.slack.circuit.foundation.onNavEvent               |
| com.slack.circuit.NavigableCircuitContent  | com.slack.circuit.foundation.NavigableCircuitContent  |
| com.slack.circuit.NavigatorDefaults        | com.slack.circuit.foundation.NavigatorDefaults        |
| com.slack.circuit.rememberCircuitNavigator | com.slack.circuit.foundation.rememberCircuitNavigator |
| com.slack.circuit.push                     | com.slack.circuit.foundation.push                     |
| com.slack.circuit.screen                   | com.slack.circuit.foundation.screen                   |

### More Highlights
- [Core] Remove Android-specific `NavigableCircuitContent` and just use common one. Back handling still runs through `BackHandler`, but is now configured in `rememberCircuitNavigator`.
- [Core] Add `defaultNavDecoration` to `CircuitConfig` to allow for customizing the default `NavDecoration` used in `NavigableCircuitContent`.
- [Core] Mark `CircuitUiState` as `@Stable` instead of `@Immutable`.
- [Code gen] Capitalize generated class names when source is a presenter function.
- [Sample] New `:samples:tacos` order builder sample to demonstrate complex state management.
- [Sample] `NavigableCircuitContent` example in the desktop counter.
- [Dependencies] Update compose to `1.4.1`.
- [Dependencies] Update compose-compiler to `1.4.4`.
- [Dependencies] Update androidx.activity to `1.7.0`.
- [Dependencies] Update molecule to `0.7.1`.

0.7.0
-----

_2023-02-10_

- **New**: Multiplatform support for `NavigableCircuitContent`! Special thanks to [@ashdavies](https://github.com/ashdavies) for contributions to make this possible.
- **Fix**: `circuit-retained` minSdk is now 21 again. We accidentally bumped it to 28 when merging in its instrumentation tests.
- **Enhancement**: embedded baseline profiles are now embedded per-artifact instead of in the root `circuit-core` artifact.
- **Enhancement**: `circuit-retained` is now covered in embedded baseline profiles.
- [Code Gen] Update Dagger to `2.45`.
- [Code Gen] Update KSP to `1.8.10-1.0.9`.
- Update to compose-compiler `1.4.2`.
- Update to Kotlin `1.8.10`.

0.6.0
-----

_2023-02-02_

Happy groundhog day!

* **Breaking API change**: `Ui.Content()` now contains a `Modifier` parameter.

  This allows you to pass modifiers on to UIs directly.

  ```diff
   public interface Ui<UiState : CircuitUiState> {
  -  @Composable public fun Content(state: UiState)
  +  @Composable public fun Content(state: UiState, modifier: Modifier)
   }
  ```

* **New:** Add `Navigator.resetRoot(Screen)` function to reset the backstack root with a new root screen. There is a corresponding `awaitResetRoot()` function added to `FakeNavigator`.
* **New:** Add `EventListener.start` callback function.
* **New:** Add Compose UI dependency to circuit-core (to support `Modifier` in the API).
* **Fix:** Fix `CircuitContext.putTag` generics.
* **Fix:** Fix KSP code gen artifact to just be a pure JVM artifact.
* **Fix:** `EventListener.onState`'s type is now `CircuitUiState` instead of `Any`.
* **Removed:** `ScreenUi` is now removed and `Ui.Factory` simply returns `Ui` instances now.
* **API Change:** `CircuitConfig.onUnavailableContent` is now no longer nullable. By default it displays a big ugly error text. If you want the previous behavior of erroring, replace it with a composable function that just throws an exception.

* Dependency updates
```
Kotlin 1.8.0
Compose-JB 1.3.0
KSP 1.8.0-1.0.9
Compose Runtime 1.3.3
Compose UI 1.3.3
Compose Animation 1.3.3
```

0.5.0
-----

_2022-12-22_

* **Enhancement**: Circuit no longer requires manual provisioning of its internal backing `ViewModel`s. This is now done automatically by the Circuit itself.
* **Enhancement**: `circuit-retained` is now fully optional and not included as a transitive dependency of circuit-core. If you want to use it, see its installation instructions in its [README](https://github.com/slackhq/circuit/tree/main/circuit-retained).
* **Enhancement**: Mark `Screen` as `@Immutable`.
* **Breaking API Change**: `LocalCircuitOwner` is now just `LocalCircuitConfig` to be more idiomatic.
* **Breaking API Change**: `LocalRetainedStateRegistryOwner` is now just `LocalRetainedStateRegistry` to be more idiomatic.
* **Breaking API Change**: `Continuity` is now `internal` and not publicly exposed since it no longer needs to be manually provided.
* **Breaking API Change**: `ViewModelBackStackRecordLocalProvider` is now `internal` and not publicly exposed since it no longer needs to be manually provided.
* **Fix**: Add missing license info to pom.
* Dependency updates
  ```toml
  [versions]
  anvil = "2.4.3"
  compose-jb = "1.2.2"
  compose-animation = "1.3.2"
  compose-compiler = "1.3.2"
  compose-foundation = "1.3.1"
  compose-material = "1.3.1"
  compose-material3 = "1.0.1"
  compose-runtime = "1.3.2"
  compose-ui = "1.3.2"
  kotlin = "1.7.22"
  ```

0.4.0
-----

_2022-12-07_

* **Breaking API Change**: `Presenter` and `Ui` factories' `create()` functions now offer a `CircuitContext` parameter in place of a `CircuitConfig` parameter. This class contains a `CircuitConfig`, a tagging API, and access to parent contexts. This allows for plumbing your own metadata through Circuit's internals such as tracing tools, logging, etc.
* **Enhancement**: New lifecycle functions added to `EventListener`.
  * `onBeforeCreatePresenter`
  * `onAfterCreatePresenter`
  * `onBeforeCreateUi`
  * `onAfterCreateUi`
  * `onUnavailableContent`
  * `onStartPresent`
  * `onDisposePresent`
  * `onStartContent`
  * `onDisposeContent`
  * `dispose`
* Update Compose to `1.3.1`.
* Update Compose (JB) to `1.2.1`.
* Update Molecule to `0.6.1`.
* Added a demo to the STAR sample that shows how to navigate to standard Android components ([#275](https://github.com/slackhq/circuit/pull/275)).

0.3.1
-----

_2022-11-07_

* **Enhancement**: Add back the `onRootPop()` parameter in `rememberCircuitNavigator()` but use `LocalOnBackPressedDispatcherOwner` for backpress handling by default.

0.3.0
-----

_2022-11-01_

* **New**: The Overlay API is now extracted to a separate, optional `circuit-overlay` artifact.
* **New**: The `circuit-core` artifact now packages in baseline profiles.
* **Enhancement**: Simplify backstack root pop handling. There is no more `onRootPop()` option in `rememberCircuitNavigator()`, instead you should install your own `BackHandler()` prior to rendering your circuit content to customize back behavior when the circuit `Navigator` is at root.
* **Fix**: `circuit-codegen-annotations` is now a multiplatform project and doesn't accidentally impose the compose-desktop dependency.

We've also updated a number of docs around code gen, overlays, and interop (including a new interop sample).

0.2.2
-----

_2022-10-27_

- **Enhancement**: Code gen now supports non-assisted constructor-injected types.
- **Enhancement**: Code gen checks that functions and classes are visible to generated factories.

0.2.1
-----

_2022-10-27_

- **Fix**: Code gen didn't properly handle instantiating simple class types.

0.2.0
-----

_2022-10-26_

- **New**: Code gen artifact. This targets specifically using Dagger + Anvil and will generate `Presenter` and `Ui.Factory` implementations for you. See `CircuitInject` for more details.
  ```kotlin
  ksp("com.slack.circuit:circuit-codegen:x.y.z")
  implementation("com.slack.circuit:circuit-codegen-annotations:x.y.z")
  ```

- **New**: There is now an `EventListener` API for instrumenting state changes for a given `Screen`. See its docs for more details.
- **Fix**: Rework `rememberRetained` implementation and support for multiple variables. Previously it only worked with one variable.
- **Fix**: Clean up some unnecessary transitive dependencies in misc artifacts.

Dependency updates

```
androidx.activity 1.6.1
androidx.compose 1.3.0
Molecule 0.5.0
```

0.1.2
-----

_2022-10-12_

- Update to compose-jb `1.2.0`.
- Update to Turbine `0.12.0`.
- **Fix**: Accidentally running molecule twice in `Presenter.test()`.

0.1.1
-----

_2022-10-10_

- **Fix**: Accidentally bundling more Compose UI dependencies than necessary.

0.1.0
-----

_2022-10-10_

Initial release, see the docs: https://slackhq.github.io/circuit/.

Note that this library is still under active development and not recommended for production use.
We'll do a more formal announcement when that time comes!
