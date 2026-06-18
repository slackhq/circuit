// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.codegen

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.kspSourcesDir
import java.io.File
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

@Suppress("LargeClass")
@OptIn(ExperimentalCompilerApi::class)
class SubCircuitSymbolProcessorTest {

  private val subCircuitStubs =
    kotlin(
      "SubCircuitStubs.kt",
      """
      package com.slack.circuit.subcircuit

      import kotlin.reflect.KClass

      interface SubCircuitOuterEvent

      interface SubCircuitUiState

      interface SubScreen<OuterEvent : SubCircuitOuterEvent>

      interface SubPresenter<OuterEvent : SubCircuitOuterEvent, State : SubCircuitUiState> {
        fun present(outerEventSink: (OuterEvent) -> Unit): State
      }

      fun interface SubPresenterFactory {
        fun create(screen: SubScreen<*>): SubPresenter<*, *>?
      }

      fun interface SubUi<State : SubCircuitUiState> {
        fun Content(state: State, modifier: androidx.compose.ui.Modifier)
      }

      fun interface SubUiFactory {
        fun create(screen: SubScreen<*>): SubUi<*>?
      }

      @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
      @Retention(AnnotationRetention.BINARY)
      annotation class SubCircuitInject(val screen: KClass<*>, val scope: KClass<*>)
      """
        .trimIndent(),
    )

  private val composableStub =
    kotlin(
      "Composable.kt",
      """
      package androidx.compose.runtime

      @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE,
        AnnotationTarget.TYPE_PARAMETER,
        AnnotationTarget.PROPERTY_GETTER
      )
      @Retention(AnnotationRetention.BINARY)
      annotation class Composable
      """
        .trimIndent(),
    )

  private val modifierStub =
    kotlin(
      "Modifier.kt",
      """
      package androidx.compose.ui

      interface Modifier {
        companion object : Modifier
      }
      """
        .trimIndent(),
    )

  private val assistedAnnotationsStub =
    kotlin(
      "AssistedAnnotations.kt",
      """
      package dagger.assisted

      annotation class Assisted
      annotation class AssistedInject
      annotation class AssistedFactory
      """
        .trimIndent(),
    )

  private val scopeStub =
    kotlin(
      "AppScope.kt",
      """
      package test

      annotation class AppScope
      """
        .trimIndent(),
    )

  private val anvilAnnotations =
    kotlin(
      "AnvilAnnotations.kt",
      """
      package com.squareup.anvil.annotations
      import kotlin.reflect.KClass

      annotation class ContributesMultibinding(val scope: KClass<*>)
      """
        .trimIndent(),
    )

  private val metroAnnotations =
    kotlin(
      "MetroAnnotations.kt",
      """
      package dev.zacsweers.metro

      annotation class Assisted
      annotation class AssistedInject
      annotation class AssistedFactory
      annotation class Inject
      """
        .trimIndent(),
    )

  private val metroContributesAnnotations =
    kotlin(
      "MetroContributesAnnotations.kt",
      """
      package dev.zacsweers.metro
      import kotlin.reflect.KClass

      annotation class ContributesIntoSet(val scope: KClass<*>)
      """
        .trimIndent(),
    )

  @Test
  fun presenterFactory_anvil() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import dagger.assisted.Assisted
          import dagger.assisted.AssistedFactory
          import dagger.assisted.AssistedInject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          class TestPresenter @AssistedInject constructor(
            @Assisted val screen: TestScreen
          ) : SubPresenter<TestEvent, TestState> {

            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }

            @SubCircuitInject(TestScreen::class, AppScope::class)
            @AssistedFactory
            interface Factory {
              fun create(screen: TestScreen): TestPresenter
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/TestPresenter_Factory_SubPresenterFactory.kt",
      expectedContent =
        """
        // Generated by SubCircuitSymbolProcessor
        package test

        import com.slack.circuit.subcircuit.SubPresenter
        import com.slack.circuit.subcircuit.SubPresenterFactory
        import com.slack.circuit.subcircuit.SubScreen
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class TestPresenter_Factory_SubPresenterFactory @Inject constructor(
          private val factory: TestPresenter.Factory,
        ) : SubPresenterFactory {
          override fun create(screen: SubScreen<*>): SubPresenter<*, *>? = if (screen is TestScreen) factory.create(screen) else null
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun uiFactory_anvil() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @Composable
          fun TestUi(state: TestState, modifier: Modifier = Modifier) {
            // UI implementation
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/TestUi_SubUiFactory.kt",
      expectedContent =
        """
        // Generated by SubCircuitSymbolProcessor
        package test

        import com.slack.circuit.subcircuit.SubScreen
        import com.slack.circuit.subcircuit.SubUi
        import com.slack.circuit.subcircuit.SubUiFactory
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class TestUi_SubUiFactory @Inject constructor() : SubUiFactory {
          override fun create(screen: SubScreen<*>): SubUi<*>? = if (screen is TestScreen) {
            SubUi<TestState> { state, modifier -> TestUi(state, modifier) }
          } else {
            null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun presenterFactory_metro() {
    assertGeneratedFile(
      codegenMode = CodegenMode.METRO,
      sourceFile =
        kotlin(
          "TestPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import dev.zacsweers.metro.Assisted
          import dev.zacsweers.metro.AssistedFactory
          import dev.zacsweers.metro.AssistedInject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          class TestPresenter @AssistedInject constructor(
            @Assisted val screen: TestScreen
          ) : SubPresenter<TestEvent, TestState> {

            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }

            @SubCircuitInject(TestScreen::class, AppScope::class)
            @AssistedFactory
            interface Factory {
              fun create(screen: TestScreen): TestPresenter
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/TestPresenter_Factory_SubPresenterFactory.kt",
      expectedContent =
        """
        // Generated by SubCircuitSymbolProcessor
        package test

        import com.slack.circuit.subcircuit.SubPresenter
        import com.slack.circuit.subcircuit.SubPresenterFactory
        import com.slack.circuit.subcircuit.SubScreen
        import dev.zacsweers.metro.ContributesIntoSet
        import dev.zacsweers.metro.Inject

        @Inject
        @ContributesIntoSet(AppScope::class)
        public class TestPresenter_Factory_SubPresenterFactory(
          private val factory: TestPresenter.Factory,
        ) : SubPresenterFactory {
          override fun create(screen: SubScreen<*>): SubPresenter<*, *>? = if (screen is TestScreen) factory.create(screen) else null
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun uiFactory_metro() {
    assertGeneratedFile(
      codegenMode = CodegenMode.METRO,
      sourceFile =
        kotlin(
          "TestUi.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @Composable
          fun TestUi(state: TestState, modifier: Modifier = Modifier) {
            // UI implementation
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/TestUi_SubUiFactory.kt",
      expectedContent =
        """
        // Generated by SubCircuitSymbolProcessor
        package test

        import com.slack.circuit.subcircuit.SubScreen
        import com.slack.circuit.subcircuit.SubUi
        import com.slack.circuit.subcircuit.SubUiFactory
        import dev.zacsweers.metro.ContributesIntoSet
        import dev.zacsweers.metro.Inject

        @Inject
        @ContributesIntoSet(AppScope::class)
        public class TestUi_SubUiFactory() : SubUiFactory {
          override fun create(screen: SubScreen<*>): SubUi<*>? = if (screen is TestScreen) {
            SubUi<TestState> { state, modifier -> TestUi(state, modifier) }
          } else {
            null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun presenterFactory_topLevel() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TopLevelPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import dagger.assisted.Assisted
          import dagger.assisted.AssistedFactory
          import dagger.assisted.AssistedInject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          class TopLevelPresenter @AssistedInject constructor(
            @Assisted val screen: TestScreen
          ) : SubPresenter<TestEvent, TestState> {

            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }
          }

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @AssistedFactory
          interface TopLevelPresenterFactory {
            fun create(screen: TestScreen): TopLevelPresenter
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/TopLevelPresenterFactory_SubPresenterFactory.kt",
      expectedContent =
        """
        // Generated by SubCircuitSymbolProcessor
        package test

        import com.slack.circuit.subcircuit.SubPresenter
        import com.slack.circuit.subcircuit.SubPresenterFactory
        import com.slack.circuit.subcircuit.SubScreen
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class TopLevelPresenterFactory_SubPresenterFactory @Inject constructor(
          private val factory: TopLevelPresenterFactory,
        ) : SubPresenterFactory {
          override fun create(screen: SubScreen<*>): SubPresenter<*, *>? = if (screen is TestScreen) factory.create(screen) else null
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun error_nonInterface() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "NotAnInterface.kt",
          """
          package test

          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          class NotAnInterface
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages)
        .contains("@SubCircuitInject on classes is only valid for @AssistedFactory interfaces")
    }
  }

  @Test
  fun error_missingAssistedFactory() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "NotAssistedFactory.kt",
          """
          package test

          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          interface NotAssistedFactory {
            fun create(): String
          }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages)
        .contains("@SubCircuitInject must be combined with @AssistedFactory on factory interfaces")
    }
  }

  @Test
  fun error_nonComposableFunction() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "NotComposable.kt",
          """
          package test

          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          fun NotComposable(state: TestState) {
            // Not a composable function
          }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages)
        .contains("@SubCircuitInject on functions is only valid for @Composable functions")
    }
  }

  @Test
  fun error_missingStateParameter() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "NoStateParameter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @Composable
          fun NoStateParameter() {
            // Missing state parameter
          }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages)
        .contains("@SubCircuitInject UI functions must have at least a state parameter")
    }
  }

  @Test
  fun error_factoryReturnsNonPresenter() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "BadFactory.kt",
          """
          package test

          import dagger.assisted.AssistedFactory
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          class NotAPresenter

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @AssistedFactory
          interface BadFactory {
            fun create(): NotAPresenter
          }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertThat(messages).contains("must implement SubPresenter")
    }
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
    val generatedFile = File(generatedSourcesDir, "kotlin/$generatedFilePath")
    if (!generatedFile.exists()) {
      throw AssertionError("No generated file found at path $generatedFilePath\n${result.messages}")
    }
    assertThat(generatedFile.readText().trim()).isEqualTo(expectedContent)
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
    codegenMode: CodegenMode = CodegenMode.ANVIL,
  ): KotlinCompilation =
    KotlinCompilation().apply {
      jvmTarget = "11"
      val diStubs =
        when (codegenMode) {
          CodegenMode.ANVIL -> listOf(assistedAnnotationsStub, anvilAnnotations)
          CodegenMode.METRO -> listOf(metroAnnotations, metroContributesAnnotations)
        }
      sources =
        sourceFiles.toList() +
          listOf(subCircuitStubs, composableStub, modifierStub, scopeStub) +
          diStubs
      inheritClassPath = true
      configureKsp {
        kspProcessorOptions += "subcircuit.codegen.mode" to codegenMode.name
        symbolProcessorProviders += SubCircuitSymbolProcessor.Provider()
      }
    }
}
