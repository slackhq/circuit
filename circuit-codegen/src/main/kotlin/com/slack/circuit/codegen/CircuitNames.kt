// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.squareup.kotlinpoet.ClassName

internal object CircuitNames {
  const val CIRCUIT_RUNTIME_BASE_PACKAGE = "com.slack.circuit.runtime"
  const val DAGGER_PACKAGE = "dagger"
  const val DAGGER_HILT_PACKAGE = "$DAGGER_PACKAGE.hilt"
  const val DAGGER_HILT_CODEGEN_PACKAGE = "$DAGGER_HILT_PACKAGE.codegen"
  const val DAGGER_MULTIBINDINGS_PACKAGE = "$DAGGER_PACKAGE.multibindings"
  const val CIRCUIT_RUNTIME_UI_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.ui"
  const val CIRCUIT_RUNTIME_SCREEN_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.screen"
  const val CIRCUIT_RUNTIME_PRESENTER_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.presenter"
  val MODIFIER = ClassName("androidx.compose.ui", "Modifier")
  const val CIRCUIT_CODEGEN_ANNOTATIONS_PACKAGE = "com.slack.circuit.codegen.annotations"
  val CIRCUIT_INJECT_ANNOTATION = ClassName(CIRCUIT_CODEGEN_ANNOTATIONS_PACKAGE, "CircuitInject")
  val MERGE_CIRCUIT_COMPONENT_ANNOTATION =
    ClassName(CIRCUIT_CODEGEN_ANNOTATIONS_PACKAGE, "MergeCircuitComponent")
  val KOTLIN_INJECT_HINT_ANNOTATION =
    ClassName("$CIRCUIT_CODEGEN_ANNOTATIONS_PACKAGE.internal", "KotlinInjectHint")
  val INTERNAL_CIRCUIT_API_ANNOTATION = ClassName("com.slack.circuit.runtime", "InternalCircuitApi")
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
  const val CIRCUIT_CODEGEN_MODE = "circuit.codegen.mode"

  object KotlinInject {
    private const val ANNOTATIONS_PACKAGE = "me.tatarka.inject.annotations"
    val INJECT = ClassName(ANNOTATIONS_PACKAGE, "Inject")
    val COMPONENT = ClassName(ANNOTATIONS_PACKAGE, "Component")
    val PROVIDES = ClassName(ANNOTATIONS_PACKAGE, "Provides")
    val INTO_SET = ClassName(ANNOTATIONS_PACKAGE, "IntoSet")
  }
}
