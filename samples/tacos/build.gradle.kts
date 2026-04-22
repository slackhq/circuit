// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.kotlin.plugin.compose)
  alias(libs.plugins.ksp)
  alias(libs.plugins.metro)
  id("circuit.base")
}

android {
  namespace = "com.slack.circuit.tacos"
  testOptions { unitTests.isIncludeAndroidResources = true }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
      "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    )
  }
}

ksp { arg("circuit.codegen.mode", "metro") }

metro { @OptIn(ExperimentalMetroGradleApi::class) enableFunctionProviders.set(true) }

dependencies {
  ksp(projects.circuitCodegen)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.appCompat)
  implementation(libs.compose.material.icons)
  implementation(libs.compose.material.material3)
  implementation(projects.circuitCodegenAnnotations)
  implementation(projects.circuitFoundation)

  implementation(libs.compose.ui.tooling.preview)
  debugImplementation(libs.compose.ui.tooling)

  testImplementation(libs.compose.ui.testing.junit)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.junit)
  testImplementation(libs.molecule.runtime)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)
  testImplementation(projects.circuitTest)

  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
}
