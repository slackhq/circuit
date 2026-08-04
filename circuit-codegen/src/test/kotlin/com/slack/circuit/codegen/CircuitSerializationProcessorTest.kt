// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

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
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class CircuitSerializationProcessorTest {
  @Test
  fun metroDataClass() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "HomeScreen.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable

          abstract class AppScope

          @CircuitSerializable(AppScope::class)
          data class HomeScreen(val userId: Long) : Screen
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/HomeScreenCircuitSerializerRegistration.kt",
      codegenMode = CodegenMode.METRO,
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.screen.CircuitSaveable
        import com.slack.circuit.serialization.CircuitSerializerRegistration
        import dev.zacsweers.metro.ContributesIntoSet
        import dev.zacsweers.metro.Inject
        import kotlinx.serialization.modules.PolymorphicModuleBuilder

        @Inject
        @ContributesIntoSet(AppScope::class)
        public class HomeScreenCircuitSerializerRegistration : CircuitSerializerRegistration {
          override fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
            builder.subclass(subclass = HomeScreen::class, serializer = HomeScreen.serializer())
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun metroNestedObjectScreen() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "ProfileScreen.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable

          abstract class AppScope

          class Navigation {
            @CircuitSerializable(AppScope::class)
            object ProfileScreen : Screen
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/Navigation_ProfileScreenCircuitSerializerRegistration.kt",
      codegenMode = CodegenMode.METRO,
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.screen.CircuitSaveable
        import com.slack.circuit.serialization.CircuitSerializerRegistration
        import dev.zacsweers.metro.ContributesIntoSet
        import dev.zacsweers.metro.Inject
        import kotlinx.serialization.modules.PolymorphicModuleBuilder

        @Inject
        @ContributesIntoSet(AppScope::class)
        public class Navigation_ProfileScreenCircuitSerializerRegistration : CircuitSerializerRegistration {
          override fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
            builder.subclass(subclass = Navigation.ProfileScreen::class, serializer = Navigation.ProfileScreen.serializer())
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun anvilDataObject() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "SettingsScreen.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable

          annotation class AppScope

          @CircuitSerializable(AppScope::class)
          data object SettingsScreen : Screen
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/SettingsScreenCircuitSerializerRegistration.kt",
      codegenMode = CodegenMode.ANVIL,
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.screen.CircuitSaveable
        import com.slack.circuit.serialization.CircuitSerializerRegistration
        import com.squareup.anvil.annotations.ContributesMultibinding
        import jakarta.inject.Inject
        import kotlinx.serialization.modules.PolymorphicModuleBuilder

        @ContributesMultibinding(AppScope::class)
        public class SettingsScreenCircuitSerializerRegistration @Inject constructor() : CircuitSerializerRegistration {
          override fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
            builder.subclass(subclass = SettingsScreen::class, serializer = SettingsScreen.serializer())
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun kotlinInjectAnvilNestedPopResult() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "DialogResult.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.PopResult
          import com.slack.circuit.serialization.CircuitSerializable

          annotation class AppScope

          class Dialog {
            @CircuitSerializable(AppScope::class)
            data class Result(val accepted: Boolean) : PopResult
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/Dialog_ResultCircuitSerializerRegistration.kt",
      codegenMode = CodegenMode.KOTLIN_INJECT_ANVIL,
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.screen.CircuitSaveable
        import com.slack.circuit.serialization.CircuitSerializerRegistration
        import kotlinx.serialization.modules.PolymorphicModuleBuilder
        import me.tatarka.inject.annotations.Inject
        import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

        @Inject
        @ContributesBinding(
          AppScope::class,
          multibinding = true,
        )
        public class Dialog_ResultCircuitSerializerRegistration : CircuitSerializerRegistration {
          override fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
            builder.subclass(subclass = Dialog.Result::class, serializer = Dialog.Result.serializer())
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun hiltRegistrationAndModule() {
    val sourceFile =
      kotlin(
        "HomeScreen.kt",
        """
        package test

        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.serialization.CircuitSerializable
        import dagger.hilt.components.SingletonComponent

        @CircuitSerializable(SingletonComponent::class)
        data class HomeScreen(val userId: Long) : Screen
        """
          .trimIndent(),
      )
    assertGeneratedFiles(
      sourceFile = sourceFile,
      codegenMode = CodegenMode.HILT,
      expectedFiles =
        mapOf(
          "test/HomeScreenCircuitSerializerRegistration.kt" to
            """
            package test

            import com.slack.circuit.runtime.screen.CircuitSaveable
            import com.slack.circuit.serialization.CircuitSerializerRegistration
            import jakarta.inject.Inject
            import kotlinx.serialization.modules.PolymorphicModuleBuilder

            public class HomeScreenCircuitSerializerRegistration @Inject constructor() : CircuitSerializerRegistration {
              override fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
                builder.subclass(subclass = HomeScreen::class, serializer = HomeScreen.serializer())
              }
            }
            """
              .trimIndent(),
          "test/HomeScreenCircuitSerializerRegistrationModule.kt" to
            """
            package test

            import com.slack.circuit.serialization.CircuitSerializerRegistration
            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.codegen.OriginatingElement
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            @OriginatingElement(topLevelClass = HomeScreen::class)
            public abstract class HomeScreenCircuitSerializerRegistrationModule {
              @Binds
              @IntoSet
              public abstract fun bindHomeScreenCircuitSerializerRegistration(homeScreenCircuitSerializerRegistration: HomeScreenCircuitSerializerRegistration): CircuitSerializerRegistration
            }
            """
              .trimIndent(),
        ),
    )
  }

  @Test
  fun customSerializer() {
    assertGeneratedFile(
      sourceFile =
        kotlin(
          "CustomScreen.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable
          import kotlinx.serialization.KSerializer
          import kotlinx.serialization.Serializable
          import kotlinx.serialization.descriptors.PrimitiveKind
          import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
          import kotlinx.serialization.descriptors.SerialDescriptor
          import kotlinx.serialization.encoding.Decoder
          import kotlinx.serialization.encoding.Encoder

          annotation class AppScope

          @CircuitSerializable(AppScope::class)
          @Serializable(with = CustomScreenSerializer::class)
          data class CustomScreen(val value: String) : Screen

          object CustomScreenSerializer : KSerializer<CustomScreen> {
            override val descriptor: SerialDescriptor =
              PrimitiveSerialDescriptor("CustomScreen", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: CustomScreen) {
              encoder.encodeString(value.value)
            }

            override fun deserialize(decoder: Decoder): CustomScreen =
              CustomScreen(decoder.decodeString())
          }
          """
            .trimIndent(),
        ),
      generatedFilePath = "test/CustomScreenCircuitSerializerRegistration.kt",
      codegenMode = CodegenMode.ANVIL,
      expectedContent =
        """
        package test

        import com.slack.circuit.runtime.screen.CircuitSaveable
        import com.slack.circuit.serialization.CircuitSerializerRegistration
        import com.squareup.anvil.annotations.ContributesMultibinding
        import jakarta.inject.Inject
        import kotlinx.serialization.modules.PolymorphicModuleBuilder

        @ContributesMultibinding(AppScope::class)
        public class CustomScreenCircuitSerializerRegistration @Inject constructor() : CircuitSerializerRegistration {
          override fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
            builder.subclass(subclass = CustomScreen::class, serializer = CustomScreen.serializer())
          }
        }
        """
          .trimIndent(),
    )
  }

  @Test
  fun expectActualScreen() {
    val compilation =
      prepareCompilation(
          kotlin(
            "OpenUrlScreen.kt",
            """
            package test

            import com.slack.circuit.runtime.screen.Screen
            import com.slack.circuit.serialization.CircuitSerializable

            abstract class AppScope

            @CircuitSerializable(AppScope::class)
            expect object OpenUrlScreen : Screen
            """
              .trimIndent(),
          ),
          kotlin(
            "OpenUrlScreen.jvm.kt",
            """
            package test

            import com.slack.circuit.runtime.screen.Screen
            import com.slack.circuit.serialization.CircuitSerializable
            import kotlinx.serialization.Serializable

            @CircuitSerializable(AppScope::class)
            @Serializable
            actual object OpenUrlScreen : Screen
            """
              .trimIndent(),
          ),
          codegenMode = CodegenMode.METRO,
        )
        .apply {
          multiplatform = true
          val commonSource = File(workingDir, "sources/OpenUrlScreen.kt")
          kotlincArguments += "-Xcommon-sources=${commonSource.absolutePath}"
        }
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    val generatedFile =
      File(
        compilation.kspSourcesDir,
        "kotlin/test/OpenUrlScreenCircuitSerializerRegistration.kt",
      )
    assertThat(generatedFile.exists()).isTrue()
    assertThat(generatedFile.readText())
      .isEqualTo(
        """
        package test

        import com.slack.circuit.runtime.screen.CircuitSaveable
        import com.slack.circuit.serialization.CircuitSerializerRegistration
        import dev.zacsweers.metro.ContributesIntoSet
        import dev.zacsweers.metro.Inject
        import kotlinx.serialization.modules.PolymorphicModuleBuilder

        @Inject
        @ContributesIntoSet(AppScope::class)
        public class OpenUrlScreenCircuitSerializerRegistration : CircuitSerializerRegistration {
          override fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
            builder.subclass(subclass = OpenUrlScreen::class, serializer = OpenUrlScreen.serializer())
          }
        }
        """
          .trimIndent() + "\n"
      )
  }

  @Test
  fun rejectsWrongSupertype() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "NotCircuitSaveable.kt",
          """
          package test

          import com.slack.circuit.serialization.CircuitSerializable

          annotation class AppScope

          @CircuitSerializable(AppScope::class)
          data class NotCircuitSaveable(val value: String)
          """
            .trimIndent(),
        ),
      expectedMessage =
        "@CircuitSerializable is only applicable to Screen and PopResult implementations.",
    )
  }

  @Test
  fun rejectsAbstractClass() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "AbstractScreen.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable

          annotation class AppScope

          @CircuitSerializable(AppScope::class)
          abstract class AbstractScreen : Screen
          """
            .trimIndent(),
        ),
      expectedMessage = "@CircuitSerializable is not applicable to abstract classes.",
    )
  }

  @Test
  fun rejectsInterface() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "ScreenContract.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable

          annotation class AppScope

          @CircuitSerializable(AppScope::class)
          interface ScreenContract : Screen
          """
            .trimIndent(),
        ),
      expectedMessage = "@CircuitSerializable is not applicable to interfaces.",
    )
  }

  @Test
  fun rejectsGenericClass() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "GenericScreen.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable

          annotation class AppScope

          @CircuitSerializable(AppScope::class)
          data class GenericScreen<T>(val value: T) : Screen
          """
            .trimIndent(),
        ),
      expectedMessage = "@CircuitSerializable is not applicable to generic classes.",
    )
  }

  @Test
  fun rejectsInaccessibleClass() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "PrivateScreen.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable

          annotation class AppScope

          private class Container {
            @CircuitSerializable(AppScope::class)
            data class PrivateScreen(val value: String) : Screen
          }
          """
            .trimIndent(),
        ),
      expectedMessage =
        "@CircuitSerializable is not applicable to private, protected, or local declarations.",
    )
  }

  @Test
  fun rejectsLocalClass() {
    assertProcessingError(
      sourceFile =
        kotlin(
          "LocalScreen.kt",
          """
          package test

          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.serialization.CircuitSerializable

          annotation class AppScope

          fun createScreen(): Screen {
            @CircuitSerializable(AppScope::class)
            data class LocalScreen(val value: String) : Screen
            return LocalScreen("value")
          }
          """
            .trimIndent(),
        ),
      expectedMessage =
        "@CircuitSerializable is not applicable to private, protected, or local declarations.",
    )
  }

  private fun assertGeneratedFile(
    sourceFile: SourceFile,
    generatedFilePath: String,
    @Language("kotlin") expectedContent: String,
    codegenMode: CodegenMode,
  ) {
    assertGeneratedFiles(
      sourceFile = sourceFile,
      codegenMode = codegenMode,
      expectedFiles = mapOf(generatedFilePath to expectedContent),
    )
  }

  private fun assertGeneratedFiles(
    sourceFile: SourceFile,
    codegenMode: CodegenMode,
    expectedFiles: Map<String, String>,
  ) {
    val compilation = prepareCompilation(sourceFile, codegenMode = codegenMode)
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    for ((path, expectedContent) in expectedFiles) {
      val generatedFile = File(compilation.kspSourcesDir, "kotlin/$path")
      assertThat(generatedFile.exists()).isTrue()
      assertThat(generatedFile.readText().trim()).isEqualTo(expectedContent.trimIndent())
    }
  }

  private fun assertProcessingError(
    sourceFile: SourceFile,
    expectedMessage: String,
  ) {
    val result = prepareCompilation(sourceFile, codegenMode = CodegenMode.ANVIL).compile()
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains(expectedMessage)
  }

  private fun prepareCompilation(
    vararg sourceFiles: SourceFile,
    codegenMode: CodegenMode,
  ): KotlinCompilation =
    KotlinCompilation().apply {
      jvmTarget = "11"
      sources = sourceFiles.toList() + codegenMode.supportSources()
      inheritClassPath = true
      compilerPluginRegistrars += SerializationComponentRegistrar()
      configureKsp {
        kspProcessorOptions += CircuitOptions.MODE to codegenMode.name
        symbolProcessorProviders += CircuitSerializationProcessor.Provider()
      }
    }

  private fun CodegenMode.supportSources(): List<SourceFile> =
    when (this) {
      CodegenMode.UNKNOWN -> error("Not possible in tests")
      CodegenMode.ANVIL ->
        listOf(
          kotlin(
            "AnvilAnnotations.kt",
            """
            package com.squareup.anvil.annotations

            import kotlin.reflect.KClass

            annotation class ContributesMultibinding(val scope: KClass<*>)
            """
              .trimIndent(),
          )
        )
      CodegenMode.HILT ->
        listOf(
          kotlin(
            "SingletonComponent.kt",
            """
            package dagger.hilt.components

            annotation class SingletonComponent
            """
              .trimIndent(),
          ),
          kotlin(
            "HiltAnnotations.kt",
            """
            package dagger.hilt

            import kotlin.reflect.KClass

            annotation class InstallIn(val value: KClass<*>)
            """
              .trimIndent(),
          ),
          kotlin(
            "Origins.kt",
            """
            package dagger.hilt.codegen

            import kotlin.reflect.KClass

            annotation class OriginatingElement(val topLevelClass: KClass<*>)
            """
              .trimIndent(),
          ),
        )
      CodegenMode.KOTLIN_INJECT_ANVIL ->
        listOf(
          kotlin(
            "KotlinInject.kt",
            """
            package me.tatarka.inject.annotations

            annotation class Inject
            """
              .trimIndent(),
          ),
          kotlin(
            "KotlinInjectAnvil.kt",
            """
            package software.amazon.lastmile.kotlin.inject.anvil

            import kotlin.reflect.KClass

            annotation class ContributesBinding(
              val scope: KClass<*>,
              val multibinding: Boolean = false,
            )
            """
              .trimIndent(),
          ),
          kotlin(
            "KotlinInjectAnvilInternal.kt",
            """
            package software.amazon.lastmile.kotlin.inject.anvil.internal

            import kotlin.reflect.KClass

            annotation class Origin(val value: KClass<*>)
            """
              .trimIndent(),
          ),
        )
      CodegenMode.METRO ->
        listOf(
          kotlin(
            "Metro.kt",
            """
            package dev.zacsweers.metro

            import kotlin.reflect.KClass

            annotation class Inject
            annotation class ContributesIntoSet(val scope: KClass<*>)
            annotation class Origin(val value: KClass<*>)
            """
              .trimIndent(),
          )
        )
    }
}
