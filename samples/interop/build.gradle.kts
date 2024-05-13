// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.plugin.parcelize)
}

android {
  namespace = "com.slack.circuit.sample.interop"
  defaultConfig {
    minSdk = 28
    targetSdk = 34
    versionCode = 1
    versionName = "1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  buildFeatures { viewBinding = true }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}

dependencies {
  implementation(projects.circuitFoundation)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.browser)
  implementation(libs.bundles.compose.ui)
  implementation(libs.androidx.compose.ui.util)
  implementation(libs.androidx.compose.integration.materialThemeAdapter)
  implementation(libs.androidx.compose.material.icons)
  implementation(libs.androidx.compose.accompanist.pager)
  implementation(libs.androidx.compose.accompanist.pager.indicators)
  implementation(libs.androidx.compose.accompanist.flowlayout)
  implementation(libs.androidx.compose.accompanist.swiperefresh)
  implementation(libs.androidx.compose.accompanist.systemUi)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.windowSizeClass)
  implementation(libs.bundles.androidx.activity)
  implementation(libs.kotlinx.immutable)
  implementation(libs.rxjava)
  implementation(libs.coroutines.rxjava)
  implementation(libs.molecule.runtime)
  implementation(libs.androidx.compose.runtime.rxjava3)

  testImplementation(libs.androidx.compose.foundation)
  testImplementation(libs.androidx.compose.ui.testing.junit)
  testImplementation(libs.androidx.compose.ui.testing.manifest)
  testImplementation(libs.androidx.test.espresso.core)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(projects.internalTestUtils)
}
