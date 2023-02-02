Changelog
=========

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
