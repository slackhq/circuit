/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  alias(libs.plugins.moshiGradlePlugin)
  alias(libs.plugins.anvil)
}

android {
  namespace = "com.slack.circuit.star"

  defaultConfig {
    minSdk = 28
    targetSdk = 32
    versionCode = 1
    versionName = "1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testApplicationId = "com.slack.circuit.star.androidTest"
  }

  testOptions { unitTests.isIncludeAndroidResources = true }
}

tasks.withType<KotlinCompile>().configureEach {
  @Suppress("SuspiciousCollectionReassignment")
  kotlinOptions {
    freeCompilerArgs +=
      listOf(
        "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
      )

    if (project.hasProperty("circuit.enableComposeCompilerReports")) {
      freeCompilerArgs +=
        listOf(
          "-P",
          "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
            project.buildDir.absolutePath +
            "/compose_metrics"
        )
      freeCompilerArgs +=
        listOf(
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
  implementation(projects.circuit)
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
  implementation(libs.leakcanary.android)
  implementation(libs.dagger)

  testImplementation(libs.androidx.compose.ui.testing.junit)
  testImplementation(libs.junit)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.molecule.runtime)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(projects.circuitTest)

  debugImplementation(libs.androidx.compose.ui.testing.manifest)
  androidTestImplementation(libs.androidx.compose.ui.testing.junit)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.coroutines.test)
  androidTestImplementation(libs.truth)
}
