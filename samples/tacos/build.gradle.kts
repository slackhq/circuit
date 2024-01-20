// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.slack.circuit.tacos"

  defaultConfig { minSdk = 28 }

  testOptions { unitTests.isIncludeAndroidResources = true }
}

tasks
  .withType<KotlinCompile>()
  .matching { it !is KaptGenerateStubsTask }
  .configureEach {
    compilerOptions {
      freeCompilerArgs.addAll(
        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
      )
    }
  }

dependencies {
  ksp(projects.circuitCodegen)

  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.compose.accompanist.systemUi)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.compose.integration.materialThemeAdapter)
  implementation(libs.androidx.compose.material.material3)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.bundles.compose.ui)
  implementation(libs.dagger)
  implementation(libs.kotlinx.immutable)
  implementation(projects.circuitFoundation)
  implementation(projects.circuitCodegenAnnotations)

  testImplementation(libs.androidx.compose.ui.testing.junit)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.junit)
  testImplementation(libs.molecule.runtime)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)
  testImplementation(projects.circuitTest)

  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.testing.espresso.core)
}
