// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.kspSourcesDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

@Suppress("LargeClass")
@OptIn(ExperimentalCompilerApi::class)
class SubCircuitDialectProcessorTest {

  private val subCircuitStubs =
    kotlin(
      "SubCircuitStubs.kt",
      """
      package com.slack.circuit.subcircuit

      import androidx.compose.runtime.Composable
      import androidx.compose.ui.Modifier
      import kotlin.reflect.KClass

      interface SubCircuitOuterEvent

      interface SubCircuitUiState

      interface SubScreen<OuterEvent : SubCircuitOuterEvent>

      interface SubPresenter<OuterEvent : SubCircuitOuterEvent, State : SubCircuitUiState> {
        @Composable fun present(outerEventSink: (OuterEvent) -> Unit): State
      }

      fun interface SubPresenterFactory {
        fun create(screen: SubScreen<*>): SubPresenter<*, *>?
      }

      fun interface SubUi<in State : SubCircuitUiState> {
        @Composable fun Content(state: State, modifier: Modifier)
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

  private val daggerStub =
    kotlin(
      "DaggerAnnotations.kt",
      """
      package dagger

      annotation class Module
      annotation class Binds
      """
        .trimIndent(),
    )

  private val daggerHiltStub =
    kotlin(
      "HiltAnnotations.kt",
      """
      package dagger.hilt

      import kotlin.reflect.KClass

      annotation class InstallIn(vararg val components: KClass<*>)
      """
        .trimIndent(),
    )

  private val daggerHiltCodegenStub =
    kotlin(
      "HiltCodegenAnnotations.kt",
      """
      package dagger.hilt.codegen

      import kotlin.reflect.KClass

      annotation class OriginatingElement(val topLevelClass: KClass<*>)
      """
        .trimIndent(),
    )

  private val daggerMultibindingsStub =
    kotlin(
      "MultibindingsAnnotations.kt",
      """
      package dagger.multibindings

      annotation class IntoSet
      """
        .trimIndent(),
    )

  private val jakartaStub =
    kotlin(
      "JakartaInject.kt",
      """
      package jakarta.inject

      annotation class Inject
      annotation class Qualifier

      interface Provider<T> {
        fun get(): T
      }
      """
        .trimIndent(),
    )

  private val javaxStub =
    kotlin(
      "JavaxInject.kt",
      """
      package javax.inject

      annotation class Inject

      interface Provider<T> {
        fun get(): T
      }
      """
        .trimIndent(),
    )

  private val metroStub =
    kotlin(
      "Metro.kt",
      """
      package dev.zacsweers.metro

      import kotlin.reflect.KClass

      annotation class Inject
      annotation class Assisted
      annotation class AssistedFactory
      annotation class Origin(val value: KClass<*>)
      annotation class ContributesIntoSet(val scope: KClass<*>)
      """
        .trimIndent(),
    )

  private val kotlinInjectStub =
    kotlin(
      "KotlinInject.kt",
      """
      package me.tatarka.inject.annotations

      annotation class Inject
      annotation class Assisted
      """
        .trimIndent(),
    )

  private val kotlinInjectAnvilStub =
    kotlin(
      "KotlinInjectAnvil.kt",
      """
      package software.amazon.lastmile.kotlin.inject.anvil

      import kotlin.reflect.KClass

      annotation class ContributesBinding(val scope: KClass<*>, val multibinding: Boolean = false)
      """
        .trimIndent(),
    )

  private val kotlinInjectAnvilOriginStub =
    kotlin(
      "KotlinInjectAnvilOrigin.kt",
      """
      package software.amazon.lastmile.kotlin.inject.anvil.internal

      import kotlin.reflect.KClass

      annotation class Origin(val value: KClass<*>)
      """
        .trimIndent(),
    )

  private val qualifierStub =
    kotlin(
      "Named.kt",
      """
      package test

      import jakarta.inject.Qualifier

      @Qualifier
      annotation class MyQualifier
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
        package test

        import com.slack.circuit.subcircuit.SubPresenter
        import com.slack.circuit.subcircuit.SubPresenterFactory
        import com.slack.circuit.subcircuit.SubScreen
        import com.squareup.anvil.annotations.ContributesMultibinding
        import jakarta.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class TestPresenter_Factory_SubPresenterFactory @Inject constructor(
          private val factory: TestPresenter.Factory,
        ) : SubPresenterFactory {
          override fun create(screen: SubScreen<*>): SubPresenter<*, *>? = when (screen) {
            is TestScreen -> factory.create(screen = screen)
            else -> null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun presenterFactory_metro() {
    assertGeneratedContains(
      sourceFile =
        kotlin(
          "MetroPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import dev.zacsweers.metro.Inject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @Inject
          class MetroPresenter : SubPresenter<TestEvent, TestState> {
            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/MetroPresenter_SubPresenterFactory.kt",
      mode = CodegenMode.METRO,
      expectedSubstrings =
        listOf(
          "import dev.zacsweers.metro.ContributesIntoSet",
          "import dev.zacsweers.metro.Inject",
          "@Inject",
          "@ContributesIntoSet(AppScope::class)",
          "is TestScreen -> provider()",
        ),
    )
  }

  @Test
  fun presenterProviderFactory_anvil() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "PlainPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import jakarta.inject.Inject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          class PlainPresenter @Inject constructor() : SubPresenter<TestEvent, TestState> {
            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/PlainPresenter_SubPresenterFactory.kt",
      expectedContent =
        """
        package test

        import com.slack.circuit.subcircuit.SubPresenter
        import com.slack.circuit.subcircuit.SubPresenterFactory
        import com.slack.circuit.subcircuit.SubScreen
        import com.squareup.anvil.annotations.ContributesMultibinding
        import jakarta.inject.Inject
        import jakarta.inject.Provider

        @ContributesMultibinding(AppScope::class)
        public class PlainPresenter_SubPresenterFactory @Inject constructor(
          private val provider: Provider<PlainPresenter>,
        ) : SubPresenterFactory {
          override fun create(screen: SubScreen<*>): SubPresenter<*, *>? = when (screen) {
            is TestScreen -> provider.get()
            else -> null
          }
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
        package test

        import com.slack.circuit.subcircuit.SubScreen
        import com.slack.circuit.subcircuit.SubUi
        import com.slack.circuit.subcircuit.SubUiFactory
        import com.squareup.anvil.annotations.ContributesMultibinding
        import jakarta.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class TestUi_SubUiFactory @Inject constructor() : SubUiFactory {
          override fun create(screen: SubScreen<*>): SubUi<*>? = when (screen) {
            is TestScreen -> SubUi<TestState> { state, modifier -> TestUi(state = state, modifier = modifier) }
            else -> null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun uiFactory_metro() {
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
      mode = CodegenMode.METRO,
      expectedContent =
        """
        package test

        import com.slack.circuit.subcircuit.SubScreen
        import com.slack.circuit.subcircuit.SubUi
        import com.slack.circuit.subcircuit.SubUiFactory
        import dev.zacsweers.metro.ContributesIntoSet
        import dev.zacsweers.metro.Inject

        @Inject
        @ContributesIntoSet(AppScope::class)
        public class TestUi_SubUiFactory : SubUiFactory {
          override fun create(screen: SubScreen<*>): SubUi<*>? = when (screen) {
            is TestScreen -> SubUi<TestState> { state, modifier -> TestUi(state = state, modifier = modifier) }
            else -> null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun uiClassFactory_anvil() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "TestUiClass.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier
          import jakarta.inject.Inject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubScreen
          import com.slack.circuit.subcircuit.SubUi

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          class TestUiClass @Inject constructor() : SubUi<TestState> {
            @Composable
            override fun Content(state: TestState, modifier: Modifier) {
              // UI implementation
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/TestUiClass_SubUiFactory.kt",
      expectedContent =
        """
        package test

        import com.slack.circuit.subcircuit.SubScreen
        import com.slack.circuit.subcircuit.SubUi
        import com.slack.circuit.subcircuit.SubUiFactory
        import com.squareup.anvil.annotations.ContributesMultibinding
        import jakarta.inject.Inject
        import jakarta.inject.Provider

        @ContributesMultibinding(AppScope::class)
        public class TestUiClass_SubUiFactory @Inject constructor(
          private val provider: Provider<TestUiClass>,
        ) : SubUiFactory {
          override fun create(screen: SubScreen<*>): SubUi<*>? = when (screen) {
            is TestScreen -> provider.get()
            else -> null
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
        package test

        import com.slack.circuit.subcircuit.SubPresenter
        import com.slack.circuit.subcircuit.SubPresenterFactory
        import com.slack.circuit.subcircuit.SubScreen
        import com.squareup.anvil.annotations.ContributesMultibinding
        import jakarta.inject.Inject

        @ContributesMultibinding(AppScope::class)
        public class TopLevelPresenterFactory_SubPresenterFactory @Inject constructor(
          private val factory: TopLevelPresenterFactory,
        ) : SubPresenterFactory {
          override fun create(screen: SubScreen<*>): SubPresenter<*, *>? = when (screen) {
            is TestScreen -> factory.create(screen = screen)
            else -> null
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun objectScreen_anvil() {
    assertGeneratedContains(
      sourceFile =
        kotlin(
          "ObjectScreenUi.kt",
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

          object TestScreen : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @Composable
          fun ObjectScreenUi(state: TestState, modifier: Modifier = Modifier) {
            // UI implementation
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/ObjectScreenUi_SubUiFactory.kt",
      mode = CodegenMode.ANVIL,
      expectedSubstrings = listOf("TestScreen -> SubUi<TestState>"),
      unexpectedSubstrings = listOf("is TestScreen ->"),
    )
  }

  @Test
  fun useJavaxOnly_anvil() {
    assertGeneratedContains(
      sourceFile =
        kotlin(
          "JavaxUi.kt",
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
          fun JavaxUi(state: TestState, modifier: Modifier = Modifier) {
            // UI implementation
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/JavaxUi_SubUiFactory.kt",
      mode = CodegenMode.ANVIL,
      processorOptions = mapOf(CircuitOptions.USE_JAVAX_ONLY to "true"),
      expectedSubstrings = listOf("import javax.inject.Inject"),
      unexpectedSubstrings = listOf("import jakarta.inject.Inject"),
    )
  }

  @Test
  fun legacySubCircuitModeOption_stillHonored() {
    // The old subcircuit-codegen artifact used `subcircuit.codegen.mode`. That artifact now
    // relocates to circuit-codegen, so the legacy key must still select the mode when the new
    // `circuit.codegen.mode` key is absent.
    val sourceFile =
      kotlin(
        "LegacyModeUi.kt",
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
        fun LegacyModeUi(state: TestState, modifier: Modifier = Modifier) {
          // UI implementation
        }
        """
          .trimIndent(),
      )
    val compilation =
      KotlinCompilation().apply {
        jvmTarget = "11"
        sources =
          listOf(
            sourceFile,
            subCircuitStubs,
            composableStub,
            modifierStub,
            assistedAnnotationsStub,
            daggerStub,
            daggerHiltStub,
            daggerHiltCodegenStub,
            daggerMultibindingsStub,
            jakartaStub,
            javaxStub,
            metroStub,
            kotlinInjectStub,
            kotlinInjectAnvilStub,
            kotlinInjectAnvilOriginStub,
            qualifierStub,
            scopeStub,
            anvilAnnotations,
          )
        inheritClassPath = true
        configureKsp {
          // Only the legacy key is set; the new `circuit.codegen.mode` is intentionally omitted.
          kspProcessorOptions += "subcircuit.codegen.mode" to "metro"
          symbolProcessorProviders += CircuitSymbolProcessorProvider()
        }
      }
    val result = compilation.compile()
    assertEquals(ExitCode.OK, result.exitCode, result.messages)
    val content =
      generatedFile(compilation, "test/LegacyModeUi_SubUiFactory.kt", result.messages).readText()
    assertContains(content, "import dev.zacsweers.metro.ContributesIntoSet")
    assertContains(content, "@ContributesIntoSet(AppScope::class)")
  }

  @Test
  fun lenient_matchesBySimpleName() {
    // With lenient matching on, the dagger.assisted.AssistedFactory annotation is matched purely by
    // simple name, so an unrelated AssistedFactory annotation still triggers generation.
    assertGeneratedContains(
      sourceFile =
        kotlin(
          "LenientPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import dagger.assisted.AssistedFactory
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          class LenientPresenter : SubPresenter<TestEvent, TestState> {
            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }

            @SubCircuitInject(TestScreen::class, AppScope::class)
            @AssistedFactory
            interface Factory {
              fun create(screen: TestScreen): LenientPresenter
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/LenientPresenter_Factory_SubPresenterFactory.kt",
      mode = CodegenMode.ANVIL,
      processorOptions = mapOf(CircuitOptions.LENIENT to "true"),
      expectedSubstrings = listOf("is TestScreen -> factory.create(screen = screen)"),
    )
  }

  @Test
  fun qualifierPropagation_anvil() {
    assertGeneratedContains(
      sourceFile =
        kotlin(
          "QualifiedPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import jakarta.inject.Inject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @MyQualifier
          @SubCircuitInject(TestScreen::class, AppScope::class)
          class QualifiedPresenter @Inject constructor() : SubPresenter<TestEvent, TestState> {
            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/QualifiedPresenter_SubPresenterFactory.kt",
      mode = CodegenMode.ANVIL,
      expectedSubstrings = listOf("@MyQualifier"),
    )
  }

  @Test
  fun hiltModule_presenter() {
    assertGeneratedContains(
      sourceFile =
        kotlin(
          "HiltPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import jakarta.inject.Inject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          class HiltPresenter @Inject constructor() : SubPresenter<TestEvent, TestState> {
            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HiltPresenter_SubPresenterFactoryModule.kt",
      mode = CodegenMode.HILT,
      expectedSubstrings =
        listOf(
          "@Module",
          "@InstallIn(AppScope::class)",
          "@Binds",
          "@IntoSet",
          ": SubPresenterFactory",
        ),
    )
  }

  @Test
  fun kotlinInjectAnvil_presenter() {
    assertGeneratedContains(
      sourceFile =
        kotlin(
          "KiPresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import me.tatarka.inject.annotations.Inject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @Inject
          class KiPresenter : SubPresenter<TestEvent, TestState> {
            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/KiPresenter_SubPresenterFactory.kt",
      mode = CodegenMode.KOTLIN_INJECT_ANVIL,
      expectedSubstrings =
        listOf(
          "import me.tatarka.inject.annotations.Inject",
          "multibinding = true",
          "() -> KiPresenter",
          "is TestScreen -> provider()",
        ),
    )
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
      assertContains(messages, "SubUi composable functions must have a Modifier parameter!")
    }
  }

  @Test
  fun error_missingModifierParameter() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "NoModifier.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          @Composable
          fun NoModifier(state: TestState) {
            // Missing modifier parameter
          }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertContains(messages, "SubUi composable functions must have a Modifier parameter!")
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
      assertContains(messages, "Factory must be for a SubUi or SubPresenter class")
    }
  }

  @Test
  fun error_privateClass() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "PrivatePresenter.kt",
          """
          package test

          import androidx.compose.runtime.Composable
          import jakarta.inject.Inject
          import com.slack.circuit.subcircuit.SubCircuitInject
          import com.slack.circuit.subcircuit.SubCircuitOuterEvent
          import com.slack.circuit.subcircuit.SubCircuitUiState
          import com.slack.circuit.subcircuit.SubPresenter
          import com.slack.circuit.subcircuit.SubScreen

          sealed interface TestEvent : SubCircuitOuterEvent

          data class TestState(val data: String) : SubCircuitUiState

          data class TestScreen(val id: String) : SubScreen<TestEvent>

          @SubCircuitInject(TestScreen::class, AppScope::class)
          private class PrivatePresenter @Inject constructor() : SubPresenter<TestEvent, TestState> {
            @Composable
            override fun present(outerEventSink: (TestEvent) -> Unit): TestState {
              return TestState("test")
            }
          }
          """
            .trimIndent(),
        )
    ) { messages ->
      assertContains(
        messages,
        "SubCircuitInject is not applicable to private or local functions and classes.",
      )
    }
  }

  private fun assertGeneratedFile(
    sourceFile: SourceFile,
    generatedFilePath: String,
    @Language("kotlin") expectedContent: String,
    mode: CodegenMode = CodegenMode.ANVIL,
  ) {
    val compilation = prepareCompilation(sourceFile, mode = mode)
    val result = compilation.compile()
    assertEquals(ExitCode.OK, result.exitCode, result.messages)
    val generatedFile = generatedFile(compilation, generatedFilePath, result.messages)
    assertEquals(expectedContent, generatedFile.readText().trim())
  }

  private fun assertGeneratedContains(
    sourceFile: SourceFile,
    generatedFilePath: String,
    mode: CodegenMode,
    expectedSubstrings: List<String>,
    unexpectedSubstrings: List<String> = emptyList(),
    processorOptions: Map<String, String> = emptyMap(),
  ) {
    val compilation =
      prepareCompilation(sourceFile, mode = mode, processorOptions = processorOptions)
    val result = compilation.compile()
    assertEquals(ExitCode.OK, result.exitCode, result.messages)
    val content = generatedFile(compilation, generatedFilePath, result.messages).readText()
    for (substring in expectedSubstrings) {
      assertContains(
        content,
        substring,
        message = "Expected generated file to contain:\n$substring\n\n$content",
      )
    }
    for (substring in unexpectedSubstrings) {
      assertTrue(
        !content.contains(substring),
        "Expected generated file NOT to contain:\n$substring\n\n$content",
      )
    }
  }

  private fun generatedFile(
    compilation: KotlinCompilation,
    generatedFilePath: String,
    messages: String,
  ): File {
    val generatedSourcesDir = compilation.kspSourcesDir
    val generatedFile = File(generatedSourcesDir, "kotlin/$generatedFilePath")
    if (!generatedFile.exists()) {
      throw AssertionError("No generated file found at path $generatedFilePath\n$messages")
    }
    return generatedFile
  }

  private fun assertProcessingError(sourceFile: SourceFile, body: (messages: String) -> Unit) {
    val compilation = prepareCompilation(sourceFile)
    val result = compilation.compile()
    assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
    body(result.messages)
  }

  private fun prepareCompilation(
    vararg sourceFiles: SourceFile,
    mode: CodegenMode = CodegenMode.ANVIL,
    processorOptions: Map<String, String> = emptyMap(),
  ): KotlinCompilation =
    KotlinCompilation().apply {
      jvmTarget = "11"
      sources =
        sourceFiles.toList() +
          listOf(
            subCircuitStubs,
            composableStub,
            modifierStub,
            assistedAnnotationsStub,
            daggerStub,
            daggerHiltStub,
            daggerHiltCodegenStub,
            daggerMultibindingsStub,
            jakartaStub,
            javaxStub,
            metroStub,
            kotlinInjectStub,
            kotlinInjectAnvilStub,
            kotlinInjectAnvilOriginStub,
            qualifierStub,
            scopeStub,
            anvilAnnotations,
          )
      inheritClassPath = true
      configureKsp {
        kspProcessorOptions += CircuitOptions.MODE to mode.name
        kspProcessorOptions += processorOptions
        symbolProcessorProviders += CircuitSymbolProcessorProvider()
      }
    }
}
