// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.squareup.kotlinpoet.ClassName

internal object CircuitNames {
  val ASSISTED = ClassName("dagger.assisted", "Assisted")
  val ASSISTED_FACTORY = ClassName("dagger.assisted", "AssistedFactory")
  val ASSISTED_INJECT = ClassName("dagger.assisted", "AssistedInject")
  val INJECT = ClassName("jakarta.inject", "Inject")
  val INJECT_JAVAX = ClassName("javax.inject", "Inject")
  val PROVIDER = ClassName("jakarta.inject", "Provider")
  val PROVIDER_JAVAX = ClassName("javax.inject", "Provider")
  const val CIRCUIT_RUNTIME_BASE_PACKAGE = "com.slack.circuit.runtime"
  const val DAGGER_PACKAGE = "dagger"
  const val DAGGER_HILT_PACKAGE = "$DAGGER_PACKAGE.hilt"
  const val DAGGER_HILT_CODEGEN_PACKAGE = "$DAGGER_HILT_PACKAGE.codegen"
  const val DAGGER_MULTIBINDINGS_PACKAGE = "$DAGGER_PACKAGE.multibindings"
  const val CIRCUIT_RUNTIME_UI_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.ui"
  const val CIRCUIT_RUNTIME_SCREEN_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.screen"
  const val CIRCUIT_RUNTIME_PRESENTER_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.presenter"
  val MODIFIER = ClassName("androidx.compose.ui", "Modifier")
  val CIRCUIT_INJECT_ANNOTATION =
    ClassName("com.slack.circuit.codegen.annotations", "CircuitInject")
  val CIRCUIT_PRESENTER = ClassName(CIRCUIT_RUNTIME_PRESENTER_PACKAGE, "Presenter")
  val CIRCUIT_PRESENTER_FACTORY = CIRCUIT_PRESENTER.nestedClass("Factory")
  val CIRCUIT_UI = ClassName(CIRCUIT_RUNTIME_UI_PACKAGE, "Ui")
  val CIRCUIT_UI_FACTORY = CIRCUIT_UI.nestedClass("Factory")
  val CIRCUIT_UI_STATE = ClassName(CIRCUIT_RUNTIME_BASE_PACKAGE, "CircuitUiState")
  val SCREEN = ClassName(CIRCUIT_RUNTIME_SCREEN_PACKAGE, "Screen")
  val NAVIGATOR = ClassName(CIRCUIT_RUNTIME_BASE_PACKAGE, "Navigator")
  val CIRCUIT_CONTEXT = ClassName(CIRCUIT_RUNTIME_BASE_PACKAGE, "CircuitContext")
  val DAGGER_MODULE = ClassName(DAGGER_PACKAGE, "Module")
  val DAGGER_BINDS = ClassName(DAGGER_PACKAGE, "Binds")
  val DAGGER_INSTALL_IN = ClassName(DAGGER_HILT_PACKAGE, "InstallIn")
  val DAGGER_ORIGINATING_ELEMENT = ClassName(DAGGER_HILT_CODEGEN_PACKAGE, "OriginatingElement")
  val DAGGER_INTO_SET = ClassName(DAGGER_MULTIBINDINGS_PACKAGE, "IntoSet")
  const val MODULE = "Module"
  const val FACTORY = "Factory"

  object KotlinInject {
    private const val ANNOTATIONS_PACKAGE = "me.tatarka.inject.annotations"
    val INJECT = ClassName(ANNOTATIONS_PACKAGE, "Inject")
    val ASSISTED = ClassName(ANNOTATIONS_PACKAGE, "Assisted")

    object Anvil {
      private const val RUNTIME_PACKAGE = "software.amazon.lastmile.kotlin.inject.anvil"
      private const val RUNTIME_INTERNAL_PACKAGE =
        "software.amazon.lastmile.kotlin.inject.anvil.internal"
      internal val CONTRIBUTES_BINDING = ClassName(RUNTIME_PACKAGE, "ContributesBinding")
      val ORIGIN = ClassName(RUNTIME_INTERNAL_PACKAGE, "Origin")
    }
  }

  object Metro {
    private const val RUNTIME_PACKAGE = "dev.zacsweers.metro"
    val INJECT = ClassName(RUNTIME_PACKAGE, "Inject")
    val ASSISTED = ClassName(RUNTIME_PACKAGE, "Assisted")
    val ASSISTED_FACTORY = ClassName(RUNTIME_PACKAGE, "AssistedFactory")
    val PROVIDER = ClassName(RUNTIME_PACKAGE, "Provider")
    val ORIGIN = ClassName(RUNTIME_PACKAGE, "Origin")
    internal val CONTRIBUTES_INTO_SET = ClassName(RUNTIME_PACKAGE, "ContributesIntoSet")
  }
}
