Changelog
=========

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
