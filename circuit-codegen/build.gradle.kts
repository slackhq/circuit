// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("jvm")
  alias(libs.plugins.ksp)
  alias(libs.plugins.mavenPublish)
}

dependencies {
  compileOnly(libs.ksp.api)
  compileOnly(libs.anvil.annotations)
  ksp(libs.autoService.ksp)
  implementation(libs.autoService.annotations)
  implementation(libs.dagger)
  implementation(libs.kotlinpoet)
  implementation(libs.kotlinpoet.ksp)

  testImplementation(libs.compose.foundation)
  testImplementation(libs.junit)
  testImplementation(libs.kct)
  testImplementation(libs.kct.ksp)
  testImplementation(libs.kotlin.compilerEmbeddable)
  testImplementation(libs.ksp)
  testImplementation(libs.ksp.api)
  testImplementation(libs.truth)
  testImplementation(projects.circuitCodegenAnnotations)
  testImplementation(projects.circuitRuntimePresenter)
  testImplementation(projects.circuitRuntimeScreen)
  testImplementation(projects.circuitRuntimeUi)
}
