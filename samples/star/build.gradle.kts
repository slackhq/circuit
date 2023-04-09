// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  alias(libs.plugins.moshiGradlePlugin)
  alias(libs.plugins.anvil)
  alias(libs.plugins.paparazzi)
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
}

android {
  namespace = "com.slack.circuit.star"

  defaultConfig {
    minSdk = 28
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testApplicationId = "com.slack.circuit.star.apk.androidTest"
  }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}

sqldelight {
  databases {
    create("StarDatabase") {
      packageName.set("com.slack.circuit.star.db")
      generateAsync.set(true)
    }
  }
}

tasks
  .withType<KotlinCompile>()
  .matching { it !is KaptGenerateStubsTask }
  .configureEach {
    compilerOptions {
      freeCompilerArgs.addAll(
        "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
      )

      if (project.hasProperty("circuit.enableComposeCompilerReports")) {
        freeCompilerArgs.addAll(
          "-P",
          "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
            project.buildDir.absolutePath +
            "/compose_metrics"
        )
        freeCompilerArgs.addAll(
          "-P",
          "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
            project.buildDir.absolutePath +
            "/compose_metrics"
        )
      }
    }
  }

dependencies {
  kapt(libs.dagger.compiler)
  ksp(projects.circuitCodegen)
  implementation(projects.circuitCodegenAnnotations)
  implementation(projects.circuitFoundation)
  implementation(projects.circuitOverlay)
  implementation(projects.circuitRetained)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.browser)
  implementation(libs.bundles.compose.ui)
  implementation(libs.androidx.compose.ui.util)
  implementation(libs.androidx.compose.integration.materialThemeAdapter)
  implementation(libs.androidx.compose.material.icons)
  implementation(libs.androidx.compose.material.iconsExtended)
  implementation(libs.androidx.compose.accompanist.pager)
  implementation(libs.androidx.compose.accompanist.pager.indicators)
  implementation(libs.androidx.compose.accompanist.flowlayout)
  implementation(libs.androidx.compose.accompanist.swiperefresh)
  implementation(libs.androidx.compose.accompanist.systemUi)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.bundles.androidx.activity)
  implementation(libs.coil)
  implementation(libs.coil.compose)
  implementation(libs.okhttp)
  implementation(libs.okhttp.loggingInterceptor)
  implementation(libs.okio)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converters.moshi)
  debugImplementation(libs.leakcanary.android)
  implementation(libs.dagger)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.immutable)
  implementation(libs.jsoup)
  implementation(libs.sqldelight.driver.android)
  implementation(libs.sqldelight.coroutines)
  implementation(libs.sqldelight.primitiveAdapters)

  testImplementation(libs.androidx.compose.ui.testing.junit)
  testImplementation(libs.junit)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.molecule.runtime)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(libs.testing.hamcrest)
  testImplementation(libs.androidx.loader)
  testImplementation(projects.circuitTest)
  testImplementation(libs.testing.espresso.core)
  testImplementation(libs.androidx.compose.ui.testing.manifest)
  testImplementation(libs.leakcanary.android.instrumentation)
  testImplementation(projects.samples.star.coilRule)

  androidTestImplementation(libs.androidx.compose.ui.testing.manifest)
  androidTestImplementation(libs.leakcanary.android.instrumentation)
  androidTestImplementation(libs.androidx.compose.ui.testing.junit)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.coroutines.test)
  androidTestImplementation(libs.truth)
  androidTestImplementation(projects.samples.star.coilRule)
}
