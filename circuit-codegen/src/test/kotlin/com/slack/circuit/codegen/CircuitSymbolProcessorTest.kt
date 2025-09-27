// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import java.io.File
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Ignore
import org.junit.Test

@Suppress("LargeClass", "RedundantVisibilityModifier")
@OptIn(ExperimentalCompilerApi::class)
class CircuitSymbolProcessorTest {
  private val appScope =
    kotlin(
      "AppScope.kt",
      """
        package test

        annotation class AppScope
      """
        .trimIndent(),
    )
  private val singletonComponent =
    kotlin(
      "SingletonComponent.kt",
      """
        package dagger.hilt.components

        annotation class SingletonComponent
      """
        .trimIndent(),
    )
  private val kotlinInjectAnnotation =
    kotlin(
      "Inject.kt",
      """
        package me.tatarka.inject.annotations

        annotation class Inject
      """
        .trimIndent(),
    )
  private val metroAnnotation =
    kotlin(
      "Inject.kt",
      """
        package dev.zacsweers.metro

        annotation class Inject
      """
        .trimIndent(),
    )
  private val metroAssistedAnnotation =
    kotlin(
      "Assisted.kt",
      """
        package dev.zacsweers.metro

        annotation class Assisted(val value: String = "")
      """
        .trimIndent(),
    )
  private val metroAssistedFactoryAnnotation =
    kotlin(
      "AssistedFactory.kt",
      """
        package dev.zacsweers.metro

        annotation class AssistedFactory
      """
        .trimIndent(),
    )
  private val screens =
    kotlin(
      "Screens.kt",
      """
        package test

        import com.slack.circuit.runtime.CircuitUiState
        import com.slack.circuit.runtime.screen.Screen

        object HomeScreen : Screen {
          data class State(val message: String) : CircuitUiState
        }
        data class FavoritesScreen(val userId: String) : Screen {
          data class State(val count: Int) : CircuitUiState
        }
      """
        .trimIndent(),
    )

  @Test
  fun simpleUiFunction_withObjectScreen_noState() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(HomeScreen::class, AppScope::class)
            @Composable
            fun Home(modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HomeFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.CircuitUiState
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.slack.circuit.runtime.ui.ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class HomeFactory @Inject constructor() : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              HomeScreen -> ui<CircuitUiState> { _, modifier -> Home(modifier = modifier) }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simpleUiFunction_withClassScreen_noState() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun Favorites(modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.CircuitUiState
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.slack.circuit.runtime.ui.ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesFactory @Inject constructor() : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              is FavoritesScreen -> ui<CircuitUiState> { _, modifier -> Favorites(modifier = modifier) }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simpleUiFunction_withInjectedClassScreen_noState() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun Favorites(screen: FavoritesScreen, modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.CircuitUiState
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.slack.circuit.runtime.ui.ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> ui<CircuitUiState> { _, modifier -> Favorites(modifier = modifier, screen = screen) }
            else -> null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simpleUiFunction_withInjectedObjectScreen_noState() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(HomeScreen::class, AppScope::class)
            @Composable
            fun Home(screen: HomeScreen, modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HomeFactory.kt",
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.CircuitUiState
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.slack.circuit.runtime.ui.ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class HomeFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            HomeScreen -> ui<CircuitUiState> { _, modifier -> Home(modifier = modifier, screen = screen as HomeScreen) }
            else -> null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simpleUiFunction_withObjectScreen_withState() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(HomeScreen::class, AppScope::class)
            @Composable
            fun Home(state: HomeScreen.State, modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HomeFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.slack.circuit.runtime.ui.ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class HomeFactory @Inject constructor() : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              HomeScreen -> ui<HomeScreen.State> { state, modifier -> Home(state = state, modifier = modifier) }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simpleUiFunction_withClassScreen_withState() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun Favorites(state: FavoritesScreen.State, modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.slack.circuit.runtime.ui.ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesFactory @Inject constructor() : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              is FavoritesScreen -> ui<FavoritesScreen.State> { state, modifier -> Favorites(state = state, modifier = modifier) }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simpleUiFunction_withInjectedClassScreen_withState() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun Favorites(state: FavoritesScreen.State, screen: FavoritesScreen, modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.slack.circuit.runtime.ui.ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> ui<FavoritesScreen.State> { state, modifier -> Favorites(state = state, modifier = modifier, screen = screen) }
            else -> null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simpleUiFunction_withInjectedObjectScreen_withState() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(HomeScreen::class, AppScope::class)
            @Composable
            fun Home(state: HomeScreen.State, screen: HomeScreen, modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HomeFactory.kt",
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.slack.circuit.runtime.ui.ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class HomeFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            HomeScreen -> ui<HomeScreen.State> { state, modifier -> Home(state = state, modifier = modifier, screen = screen as HomeScreen) }
            else -> null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun uiClass_noInjection() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.ui.Ui
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            class Favorites : Ui<FavoritesScreen.State> {
              @Composable
              override fun Content(state: FavoritesScreen.State, modifier: Modifier) {

              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesFactory @Inject constructor() : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              is FavoritesScreen -> Favorites()
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun uiClass_simpleInjection() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.ui.Ui
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import javax.inject.Inject

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            class Favorites @Inject constructor() : Ui<FavoritesScreen.State> {
              @Composable
              override fun Content(state: FavoritesScreen.State, modifier: Modifier) {

              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject
          import javax.inject.Provider

          @ContributesMultibinding(AppScope::class)
          public class FavoritesFactory @Inject constructor(
            private val provider: Provider<Favorites>,
          ) : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              is FavoritesScreen -> provider.get()
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun uiClass_simpleInjection_secondaryConstructor() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.ui.Ui
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import javax.inject.Inject

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            class Favorites(val value: String) : Ui<FavoritesScreen.State> {

              @Inject constructor() : this("injected")

              @Composable
              override fun Content(state: FavoritesScreen.State, modifier: Modifier) {

              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject
          import javax.inject.Provider

          @ContributesMultibinding(AppScope::class)
          public class FavoritesFactory @Inject constructor(
            private val provider: Provider<Favorites>,
          ) : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              is FavoritesScreen -> provider.get()
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun uiClass_assistedInjection() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.CircuitContext
            import com.slack.circuit.runtime.ui.Ui
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import dagger.assisted.Assisted
            import dagger.assisted.AssistedFactory
            import dagger.assisted.AssistedInject

            class Favorites @AssistedInject constructor(
              @Assisted private val screen: FavoritesScreen,
              @Assisted private val circuitContext: CircuitContext,
            ) : Ui<FavoritesScreen.State> {
              @CircuitInject(FavoritesScreen::class, AppScope::class)
              @AssistedFactory
              fun interface Factory {
                fun create(screen: FavoritesScreen, circuitContext: CircuitContext): Favorites
              }

              @Composable
              override fun Content(state: FavoritesScreen.State, modifier: Modifier) {

              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesFactory @Inject constructor(
            private val factory: Favorites.Factory,
          ) : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              is FavoritesScreen -> factory.create(screen = screen, circuitContext = context)
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simplePresenterFunction_withObjectScreen() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable

            @CircuitInject(HomeScreen::class, AppScope::class)
            @Composable
            fun HomePresenter(): HomeScreen.State {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HomePresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.presenter.presenterOf
          import com.slack.circuit.runtime.screen.Screen
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class HomePresenterFactory @Inject constructor() : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              HomeScreen -> presenterOf { HomePresenter() }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simplePresenterFunction_withClassScreen() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun FavoritesPresenter(): FavoritesScreen.State {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.presenter.presenterOf
          import com.slack.circuit.runtime.screen.Screen
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesPresenterFactory @Inject constructor() : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> presenterOf { FavoritesPresenter() }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simplePresenterFunction_withInjectedClassScreen() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.Navigator
            import androidx.compose.runtime.Composable

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun FavoritesPresenter(screen: FavoritesScreen, navigator: Navigator): FavoritesScreen.State {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.presenter.presenterOf
          import com.slack.circuit.runtime.screen.Screen
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesPresenterFactory @Inject constructor() : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> presenterOf { FavoritesPresenter(screen = screen, navigator = navigator) }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun simplePresenterFunction_withInjectedObjectScreen() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.Navigator
            import androidx.compose.runtime.Composable

            @CircuitInject(HomeScreen::class, AppScope::class)
            @Composable
            fun HomePresenter(screen: HomeScreen, navigator: Navigator): HomeScreen.State {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HomePresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.presenter.presenterOf
          import com.slack.circuit.runtime.screen.Screen
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class HomePresenterFactory @Inject constructor() : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              HomeScreen -> presenterOf { HomePresenter(screen = screen as HomeScreen, navigator = navigator) }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun presenterClass_noInjection() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.presenter.Presenter
            import androidx.compose.runtime.Composable

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            class FavoritesPresenter : Presenter<FavoritesScreen.State> {
              @Composable
              override fun present(): FavoritesScreen.State {
                throw NotImplementedError()
              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.screen.Screen
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesPresenterFactory @Inject constructor() : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> FavoritesPresenter()
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun presenterClass_simpleInjection() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.presenter.Presenter
            import androidx.compose.runtime.Composable
            import javax.inject.Inject

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            class FavoritesPresenter @Inject constructor() : Presenter<FavoritesScreen.State> {
              @Composable
              override fun present(): FavoritesScreen.State {
                throw NotImplementedError()
              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.screen.Screen
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject
          import javax.inject.Provider

          @ContributesMultibinding(AppScope::class)
          public class FavoritesPresenterFactory @Inject constructor(
            private val provider: Provider<FavoritesPresenter>,
          ) : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> provider.get()
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun presenterClass_simpleInjection_kotlinInjectAnvil() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import com.slack.circuit.runtime.presenter.Presenter
          import androidx.compose.runtime.Composable
          import me.tatarka.inject.annotations.Inject

          @Inject
          @CircuitInject(FavoritesScreen::class, AppScope::class)
          class FavoritesPresenter : Presenter<FavoritesScreen.State> {
            @Composable
            override fun present(): FavoritesScreen.State {
              throw NotImplementedError()
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.screen.Screen
          import me.tatarka.inject.annotations.Inject
          import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

          @Inject
          @ContributesBinding(
            AppScope::class,
            multibinding = true,
          )
          public class FavoritesPresenterFactory(
            private val provider: () -> FavoritesPresenter,
          ) : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> provider()
              else -> null
            }
          }
        """
          .trimIndent(),
      codegenMode = CodegenMode.KOTLIN_INJECT_ANVIL,
    )
  }

  @Test
  fun presenterClass_assistedInjection() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.Navigator
            import com.slack.circuit.runtime.presenter.Presenter
            import androidx.compose.runtime.Composable
            import dagger.assisted.Assisted
            import dagger.assisted.AssistedFactory
            import dagger.assisted.AssistedInject

            class FavoritesPresenter @AssistedInject constructor(
              @Assisted private val screen: FavoritesScreen,
              @Assisted private val navigator: Navigator,
            ) : Presenter<FavoritesScreen.State> {
              @CircuitInject(FavoritesScreen::class, AppScope::class)
              @AssistedFactory
              fun interface Factory {
                fun create(screen: FavoritesScreen, navigator: Navigator): FavoritesPresenter
              }

              @Composable
              override fun present(): FavoritesScreen.State {
                throw NotImplementedError()
              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.screen.Screen
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesPresenterFactory @Inject constructor(
            private val factory: FavoritesPresenter.Factory,
          ) : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> factory.create(screen = screen, navigator = navigator)
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun presenterClass_assistedInjectionWithCircuitContext() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.CircuitContext
            import com.slack.circuit.runtime.Navigator
            import com.slack.circuit.runtime.presenter.Presenter
            import androidx.compose.runtime.Composable
            import dagger.assisted.Assisted
            import dagger.assisted.AssistedFactory
            import dagger.assisted.AssistedInject

            class FavoritesPresenter @AssistedInject constructor(
              @Assisted private val screen: FavoritesScreen,
              @Assisted private val navigator: Navigator,
              @Assisted private val circuitContext: CircuitContext
            ) : Presenter<FavoritesScreen.State> {
              @CircuitInject(FavoritesScreen::class, AppScope::class)
              @AssistedFactory
              fun interface Factory {
                fun create(screen: FavoritesScreen, navigator: Navigator, circuitContext: CircuitContext): FavoritesPresenter
              }

              @Composable
              override fun present(): FavoritesScreen.State {
                throw NotImplementedError()
              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.screen.Screen
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class FavoritesPresenterFactory @Inject constructor(
            private val factory: FavoritesPresenter.Factory,
          ) : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> factory.create(screen = screen, navigator = navigator, circuitContext = context)
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun hiltCodegenMode_skipsAnvilBinding() {
    assertGeneratedFile(
      codegenMode = CodegenMode.HILT,
      sourceFile =
        kotlin(
          "TestPresenterWithoutAnvil.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.Navigator
            import com.slack.circuit.runtime.presenter.Presenter
            import androidx.compose.runtime.Composable
            import dagger.assisted.Assisted
            import dagger.assisted.AssistedFactory
            import dagger.assisted.AssistedInject
            import dagger.hilt.components.SingletonComponent

            class FavoritesPresenter @AssistedInject constructor(
              @Assisted private val screen: FavoritesScreen,
              @Assisted private val navigator: Navigator,
            ) : Presenter<FavoritesScreen.State> {
              @CircuitInject(FavoritesScreen::class, SingletonComponent::class)
              @AssistedFactory
              fun interface Factory {
                fun create(screen: FavoritesScreen, navigator: Navigator): FavoritesPresenter
              }

              @Composable
              override fun present(): FavoritesScreen.State {
                throw NotImplementedError()
              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.screen.Screen
          import javax.inject.Inject

          public class FavoritesPresenterFactory @Inject constructor(
            private val factory: FavoritesPresenter.Factory,
          ) : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> factory.create(screen = screen, navigator = navigator)
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun hiltCodegenMode_module() {
    assertGeneratedFile(
      codegenMode = CodegenMode.HILT,
      sourceFile =
        kotlin(
          "TestPresenterWithoutAnvil.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.Navigator
            import com.slack.circuit.runtime.presenter.Presenter
            import androidx.compose.runtime.Composable
            import dagger.assisted.Assisted
            import dagger.assisted.AssistedFactory
            import dagger.assisted.AssistedInject
            import dagger.hilt.components.SingletonComponent

            class FavoritesPresenter @AssistedInject constructor(
              @Assisted private val screen: FavoritesScreen,
              @Assisted private val navigator: Navigator,
            ) : Presenter<FavoritesScreen.State> {
              @CircuitInject(FavoritesScreen::class, SingletonComponent::class)
              @AssistedFactory
              fun interface Factory {
                fun create(screen: FavoritesScreen, navigator: Navigator): FavoritesPresenter
              }

              @Composable
              override fun present(): FavoritesScreen.State {
                throw NotImplementedError()
              }
            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactoryModule.kt",
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.presenter.Presenter
        import dagger.Binds
        import dagger.Module
        import dagger.hilt.InstallIn
        import dagger.hilt.codegen.OriginatingElement
        import dagger.hilt.components.SingletonComponent
        import dagger.multibindings.IntoSet

        @Module
        @InstallIn(SingletonComponent::class)
        @OriginatingElement(topLevelClass = FavoritesPresenter::class)
        public abstract class FavoritesPresenterFactoryModule {
          @Binds
          @IntoSet
          public abstract fun bindFavoritesPresenterFactory(favoritesPresenterFactory: FavoritesPresenterFactory): Presenter.Factory
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun hiltCodegenMode_topLevelOriginatingElement() {
    assertGeneratedFile(
      codegenMode = CodegenMode.HILT,
      sourceFile =
        kotlin(
          "Home.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import dagger.hilt.components.SingletonComponent

            @CircuitInject(HomeScreen::class, SingletonComponent::class)
            @Composable
            fun Home(modifier: Modifier = Modifier) {

            }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HomeFactoryModule.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.ui.Ui
          import dagger.Binds
          import dagger.Module
          import dagger.hilt.InstallIn
          import dagger.hilt.components.SingletonComponent
          import dagger.multibindings.IntoSet

          @Module
          @InstallIn(SingletonComponent::class)
          public abstract class HomeFactoryModule {
            @Binds
            @IntoSet
            public abstract fun bindHomeFactory(homeFactory: HomeFactory): Ui.Factory
          }
        """
          .trimIndent(),
    )
  }

  @Ignore("Toe hold for when we implement this validation")
  @Test
  fun invalidInjections() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "InvalidInjections.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.presenter.Presenter
            import com.slack.circuit.runtime.ui.Ui
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import dagger.assisted.Assisted
            import dagger.assisted.AssistedFactory
            import dagger.assisted.AssistedInject

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun FavoritesFunction(state: FavoritesScreen.State, someString: String, modifier: Modifier = Modifier) {

            }

            class Favorites @AssistedInject constructor(
              @Assisted private val someString: String,
            ) : Ui<FavoritesScreen.State> {
              @CircuitInject(FavoritesScreen::class, AppScope::class)
              @AssistedFactory
              fun interface Factory {
                fun create(someString: String): Favorites
              }

              @Composable
              override fun Content(state: FavoritesScreen.State, modifier: Modifier) {

              }
            }

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun FavoritesFunctionPresenter(someString: String): FavoritesScreen.State {

            }

            class FavoritesPresenter @AssistedInject constructor(
              @Assisted private val someString: String,
            ) : Presenter<FavoritesScreen.State> {
              @CircuitInject(FavoritesScreen::class, AppScope::class)
              @AssistedFactory
              fun interface Factory {
                fun create(someString: String): FavoritesPresenter
              }

              @Composable
              override fun present(): FavoritesScreen.State {
                throw NotImplementedError()
              }
            }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages).contains("TODO")
    }
  }

  @Test
  fun invalidSupertypes() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "InvalidSupertypes.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            class Favorites {
              @Composable
              fun Content(state: FavoritesScreen.State, modifier: Modifier) {

              }
            }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages)
        .contains("Factory must be for a UI or Presenter class, but was test.Favorites.")
    }
  }

  @Ignore("Toe hold for when we implement this validation")
  @Test
  fun presenterFunctionMissingReturn() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "MissingPresenterReturn.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun FavoritesPresenter() {

            }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages).contains("TODO")
    }
  }

  @Test
  fun uiFunctionMissingModifier() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "MissingModifierParam.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @Composable
            fun Favorites() {

            }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages).contains("UI composable functions must have a Modifier parameter!")
    }
  }

  @Test
  fun invalidAssistedInjection() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "InvalidAssistedInjection.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import dagger.assisted.Assisted
            import dagger.assisted.AssistedInject
            import dagger.assisted.AssistedFactory

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            class Favorites @AssistedInject constructor(@Assisted input: String) : Presenter<FavoritesScreen.State> {
              @Composable
              override fun present(): FavoritesScreen.State {

              }

              @AssistedFactory
              fun interface Factory {
                fun create(input: String): Favorites
              }
            }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages)
        .contains(
          "When using @CircuitInject with an @AssistedInject-annotated class, you must put " +
            "the @CircuitInject annotation on the @AssistedFactory-annotated nested" +
            " class (test.Favorites.Factory)."
        )
    }
  }

  @Test
  fun invalidAssistedInjection_missingFactory() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "InvalidAssistedInjection.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import androidx.compose.runtime.Composable
            import dagger.assisted.Assisted
            import dagger.assisted.AssistedInject

            @CircuitInject(FavoritesScreen::class, AppScope::class)
            class Favorites @AssistedInject constructor(@Assisted input: String) : Presenter<FavoritesScreen.State> {
              @Composable
              override fun present(): FavoritesScreen.State {

              }
            }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages)
        .contains(
          "When using @CircuitInject with an @AssistedInject-annotated class, you must put " +
            "the @CircuitInject annotation on the @AssistedFactory-annotated nested class."
        )
    }
  }

  // Regression test for https://github.com/slackhq/circuit/issues/1704
  @Test
  fun parameterOrderDoesNotMatter() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "ParameterOrdering.kt",
          """
            package test

            import com.slack.circuit.codegen.annotations.CircuitInject
            import com.slack.circuit.runtime.screen.StaticScreen
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            data object Static : StaticScreen

            @CircuitInject(Static::class, AppScope::class)
            @Composable
            fun StaticUi(screen: Static, modifier: Modifier) {}
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/StaticUiFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.CircuitUiState
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.slack.circuit.runtime.ui.ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject

          @ContributesMultibinding(AppScope::class)
          public class StaticUiFactory @Inject constructor() : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              Static -> ui<CircuitUiState> { _, modifier -> StaticUi(modifier = modifier, screen = screen as Static) }
              else -> null
            }
          }
        """
          .trimIndent(),
    )
  }

  @Test
  fun presenterClass_simpleInjection_metro() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "FavoritesPresenter.kt",
          """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import com.slack.circuit.runtime.presenter.Presenter
          import androidx.compose.runtime.Composable
          import dev.zacsweers.metro.Inject

          @Inject
          @CircuitInject(FavoritesScreen::class, AppScope::class)
          class FavoritesPresenter : Presenter<FavoritesScreen.State> {
            @Composable
            override fun present(): FavoritesScreen.State {
              throw NotImplementedError()
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.screen.Screen
          import dev.zacsweers.metro.ContributesIntoSet
          import dev.zacsweers.metro.Inject
          import dev.zacsweers.metro.Provider

          @Inject
          @ContributesIntoSet(AppScope::class)
          public class FavoritesPresenterFactory(
            private val provider: Provider<FavoritesPresenter>,
          ) : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> provider()
              else -> null
            }
          }
        """
          .trimIndent(),
      codegenMode = CodegenMode.METRO,
    )
  }

  @Test
  fun presenterClass_assistedInjection_metro() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "FavoritesPresenter.kt",
          """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import androidx.compose.runtime.Composable
          import dev.zacsweers.metro.Assisted
          import dev.zacsweers.metro.AppScope
          import dev.zacsweers.metro.AssistedFactory
          import dev.zacsweers.metro.Inject

          @Inject
          class FavoritesPresenter(
            @Assisted private val navigator: Navigator
          ) : Presenter<FavoritesScreen.State> {
            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @AssistedFactory
            fun interface Factory {
              fun create(@Assisted navigator: Navigator): FavoritesPresenter
            }

            @Composable
            override fun present(): FavoritesScreen.State {
              throw NotImplementedError()
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/FavoritesPresenterFactory.kt",
      expectedContent =
        """
          package test

          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.Navigator
          import com.slack.circuit.runtime.presenter.Presenter
          import com.slack.circuit.runtime.screen.Screen
          import dev.zacsweers.metro.ContributesIntoSet
          import dev.zacsweers.metro.Inject

          @Inject
          @ContributesIntoSet(AppScope::class)
          public class FavoritesPresenterFactory(
            private val factory: FavoritesPresenter.Factory,
          ) : Presenter.Factory {
            override fun create(
              screen: Screen,
              navigator: Navigator,
              context: CircuitContext,
            ): Presenter<*>? = when (screen) {
              is FavoritesScreen -> factory.create(navigator = navigator)
              else -> null
            }
          }
        """
          .trimIndent(),
      codegenMode = CodegenMode.METRO,
    )
  }

  private fun assertGeneratedFile(
    sourceFile: SourceFile,
    generatedFilePath: String,
    @Language("kotlin") expectedContent: String,
    codegenMode: CodegenMode = CodegenMode.ANVIL,
  ) {
    val compilation = prepareCompilation(sourceFile, codegenMode = codegenMode)
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    val generatedSourcesDir = compilation.kspSourcesDir
    val generatedAdapter = File(generatedSourcesDir, "kotlin/$generatedFilePath")
    assertThat(generatedAdapter.exists()).isTrue()
    assertThat(generatedAdapter.readText().trim()).isEqualTo(expectedContent.trimIndent())
  }

  private fun assertProcessingError(
    sourceFile: SourceFile,
    codegenMode: CodegenMode = CodegenMode.ANVIL,
    body: (messages: String) -> Unit,
  ) {
    val compilation = prepareCompilation(sourceFile, codegenMode = codegenMode)
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    body(result.messages)
  }

  private fun prepareCompilation(
    vararg sourceFiles: SourceFile,
    codegenMode: CodegenMode,
  ): KotlinCompilation =
    KotlinCompilation().apply {
      sources =
        sourceFiles.toList() +
          screens +
          when (codegenMode) {
            CodegenMode.ANVIL -> listOf(appScope)
            CodegenMode.HILT -> listOf(singletonComponent)
            CodegenMode.KOTLIN_INJECT_ANVIL -> {
              listOf(appScope, kotlinInjectAnnotation)
            }
            CodegenMode.METRO ->
              listOf(
                appScope,
                metroAnnotation,
                metroAssistedAnnotation,
                metroAssistedFactoryAnnotation,
              )
          }
      inheritClassPath = true
      symbolProcessorProviders += CircuitSymbolProcessorProvider()
      kspProcessorOptions += "circuit.codegen.mode" to codegenMode.name
      // Necessary for KSP
      languageVersion = "1.9"
    }

  private fun compile(vararg sourceFiles: SourceFile, codegenMode: CodegenMode): CompilationResult {
    return prepareCompilation(*sourceFiles, codegenMode = codegenMode).compile()
  }
}
