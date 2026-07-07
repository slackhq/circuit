// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.codegen

import com.squareup.kotlinpoet.ClassName

internal object SubCircuitNames {
  val ASSISTED = ClassName("dagger.assisted", "Assisted")
  val ASSISTED_FACTORY = ClassName("dagger.assisted", "AssistedFactory")
  val ASSISTED_INJECT = ClassName("dagger.assisted", "AssistedInject")
  val INJECT = ClassName("jakarta.inject", "Inject")
  val INJECT_JAVAX = ClassName("javax.inject", "Inject")
  val PROVIDER = ClassName("jakarta.inject", "Provider")
  val PROVIDER_JAVAX = ClassName("javax.inject", "Provider")
  const val DAGGER_PACKAGE = "dagger"
  const val DAGGER_HILT_PACKAGE = "$DAGGER_PACKAGE.hilt"
  const val DAGGER_HILT_CODEGEN_PACKAGE = "$DAGGER_HILT_PACKAGE.codegen"
  const val DAGGER_MULTIBINDINGS_PACKAGE = "$DAGGER_PACKAGE.multibindings"
  const val SUB_CIRCUIT_PACKAGE = "com.slack.circuit.subcircuit"
  val MODIFIER = ClassName("androidx.compose.ui", "Modifier")
  val SUB_CIRCUIT_INJECT_ANNOTATION = ClassName(SUB_CIRCUIT_PACKAGE, "SubCircuitInject")
  val SUB_PRESENTER = ClassName(SUB_CIRCUIT_PACKAGE, "SubPresenter")
  val SUB_PRESENTER_FACTORY = ClassName(SUB_CIRCUIT_PACKAGE, "SubPresenterFactory")
  val SUB_UI = ClassName(SUB_CIRCUIT_PACKAGE, "SubUi")
  val SUB_UI_FACTORY = ClassName(SUB_CIRCUIT_PACKAGE, "SubUiFactory")
  val SUB_CIRCUIT_UI_STATE = ClassName(SUB_CIRCUIT_PACKAGE, "SubCircuitUiState")
  val SUB_SCREEN = ClassName(SUB_CIRCUIT_PACKAGE, "SubScreen")
  val DAGGER_MODULE = ClassName(DAGGER_PACKAGE, "Module")
  val DAGGER_BINDS = ClassName(DAGGER_PACKAGE, "Binds")
  val DAGGER_INSTALL_IN = ClassName(DAGGER_HILT_PACKAGE, "InstallIn")
  val DAGGER_ORIGINATING_ELEMENT = ClassName(DAGGER_HILT_CODEGEN_PACKAGE, "OriginatingElement")
  val DAGGER_ORIGIN = OriginAnnotation(DAGGER_ORIGINATING_ELEMENT, "topLevelClass")
  val DAGGER_INTO_SET = ClassName(DAGGER_MULTIBINDINGS_PACKAGE, "IntoSet")
  const val MODULE = "Module"
  const val SUB_PRESENTER_FACTORY_SUFFIX = "_SubPresenterFactory"
  const val SUB_UI_FACTORY_SUFFIX = "_SubUiFactory"

  /**
   * Qualified names of annotations that mark an annotation as a qualifier. Any annotation
   * meta-annotated with one of these is propagated from the annotated declaration to the generated
   * factory class.
   */
  val QUALIFIER_ANNOTATION_NAMES =
    setOf(
      "javax.inject.Qualifier",
      "jakarta.inject.Qualifier",
      "dev.zacsweers.metro.Qualifier",
      "me.tatarka.inject.annotations.Qualifier",
    )

  /**
   * FQCNs of provider/lazy types that are already deferrable references to a dependency. When a
   * function parameter already has one of these types (or is a Kotlin function type), the codegen
   * passes it through to the factory constructor as-is instead of re-wrapping it in a provider.
   */
  val PASS_THROUGH_PROVIDER_NAMES =
    setOf(
      "javax.inject.Provider",
      "jakarta.inject.Provider",
      "dev.zacsweers.metro.Provider",
      "kotlin.Function0",
      "dagger.Lazy",
      "kotlin.Lazy",
    )

  object KotlinInject {
    private const val ANNOTATIONS_PACKAGE = "me.tatarka.inject.annotations"
    val INJECT = ClassName(ANNOTATIONS_PACKAGE, "Inject")
    val ASSISTED = ClassName(ANNOTATIONS_PACKAGE, "Assisted")

    object Anvil {
      private const val RUNTIME_PACKAGE = "software.amazon.lastmile.kotlin.inject.anvil"
      private const val RUNTIME_INTERNAL_PACKAGE =
        "software.amazon.lastmile.kotlin.inject.anvil.internal"
      internal val CONTRIBUTES_BINDING = ClassName(RUNTIME_PACKAGE, "ContributesBinding")
      val ORIGIN = OriginAnnotation(ClassName(RUNTIME_INTERNAL_PACKAGE, "Origin"))
    }
  }

  object Metro {
    private const val RUNTIME_PACKAGE = "dev.zacsweers.metro"
    val INJECT = ClassName(RUNTIME_PACKAGE, "Inject")
    val ASSISTED = ClassName(RUNTIME_PACKAGE, "Assisted")
    val ASSISTED_FACTORY = ClassName(RUNTIME_PACKAGE, "AssistedFactory")
    val ORIGIN = OriginAnnotation(ClassName(RUNTIME_PACKAGE, "Origin"))
    internal val CONTRIBUTES_INTO_SET = ClassName(RUNTIME_PACKAGE, "ContributesIntoSet")
  }
}
