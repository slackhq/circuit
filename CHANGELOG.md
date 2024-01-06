Changelog
=========

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

## **New**: circuitx-effects artifact

The circuitx-effects artifact provides some effects for use with logging/analytics. These effects
are typically used in Circuit presenters for tracking `impressions` and will run only once until
forgotten based on the current circuit-retained strategy.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-effects:<version>")
}
```

Docs: https://slackhq.github.io/circuit/circuitx/#effects

## **New**: Add codegen mode to support both Anvil and Hilt

Circuit's code gen artifact now supports generating for Hilt projects. See the docs for usage instructions: https://slackhq.github.io/circuit/code-gen/

## Misc

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

## **New**: Allow retained state to be retained whilst UIs and Presenters are on the back stack.

Originally, `circuit-retained` was implemented as a solution for preserving arbitrary data across configuration changes on Android. With this change it now also acts as a solution for retaining state _across the back stack_, meaning that traversing the backstack no longer causes restored contents to re-run through their empty states anymore.

To support this, each back stack entry now has its own `RetainedStateRegistry` instance.

Note that `circuit-retained` is still optional for now, but we are considering making it part of `CircuitCompositionLocals` in the future. Please let us know your thoughts in this issue: https://github.com/slackhq/circuit/issues/891.

Full details + demos can be found in https://github.com/slackhq/circuit/pull/888. Big thank you to [@chrisbanes](https://github.com/chrisbanes) for the implementation!

## Other changes

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

## Preliminary support for iOS targets

Following the announcement of Compose for iOS alpha, this release adds `ios()` and `iosSimulatorArm64()` targets for the Circuit core artifacts. Note that this support doesn't come with any extra APIs yet for iOS, just basic target support only. We're not super sure what direction we want to take with iOS, but encourage others to try it out and let us know what patterns you like. We have updated the Counter sample to include an iOS app target as well, using Circuit for the presentation layer only and SwiftUI for the UI.

Note that circuit-codegen and circuit-codegen-annotations don't support these yet, as Anvil and Dagger only support JVM targets.

More details can be found in the PR: https://github.com/slackhq/circuit/pull/583

## Misc

- Use new baseline profile plugin for generating baseline profiles.
- Misc sample app fixes and updates.
- Add window size class example to STAR sample.
- Switch to Roborazzi for screenshot test samples.
- Small documentation updates.
- Add bi-directional Flow/Circuit interop to interop sample.

Note that we unintentionally used an experimental animation API for `NavigatorDefaults.DefaultDecotration`, which may cause R8 issues if you use a newer, experimental version of Compose animation. To avoid issues, copy the animation code and use your own copy compiled against the newest animation APIs. We'll fix this after Compose 1.5.0 is released.

## Dependency updates

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

## [Core] Split up core artifacts.
- `circuit-runtime`: common runtime components like `Screen`, `Navigator`, etc.
- `circuit-runtime-presenter`: the `Presenter` API, depends on `circuit-runtime`.
- `circuit-runtime-ui`: the `Ui` API, depends on `circuit-runtime`.
- `circuit-foundation`: the circuit foundational APIs like `CircuitConfig`, `CircuitContent`, etc. Depends on the first three.

The goal in this is to allow more granular dependencies and easier building against subsets of the API. For example, this would allow a presenter implementation to easily live in a standalone module that doesn't depend on any UI dependencies. Vice versa for UI implementations.

Where we think this could really shine is in multiplatform projects where Circuit's UI APIs may be more or less abstracted away in service of using native UI, like in iOS.

### `circuit-runtime` artifact
| Before                           | After                                    |
|----------------------------------|------------------------------------------|
| com.slack.circuit.CircuitContext | com.slack.circuit.runtime.CircuitContext |
| com.slack.circuit.CircuitUiState | com.slack.circuit.runtime.CircuitUiState |
| com.slack.circuit.CircuitUiEvent | com.slack.circuit.runtime.CircuitUiEvent |
| com.slack.circuit.Navigator      | com.slack.circuit.runtime.Navigator      |
| com.slack.circuit.Screen         | com.slack.circuit.runtime.Screen         |

### `circuit-runtime-presenter` artifact
| Before                      | After                                         |
|-----------------------------|-----------------------------------------------|
| com.slack.circuit.Presenter | com.slack.circuit.runtime.presenter.Presenter |

### `circuit-runtime-ui` artifact
| Before               | After                                  |
|----------------------|----------------------------------------|
| com.slack.circuit.Ui | com.slack.circuit.runtime.presenter.Ui |

### `circuit-foundation` artifact
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

## More Highlights
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
