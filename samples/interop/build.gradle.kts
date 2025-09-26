// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.kotlin.plugin.compose)
}

android {
  namespace = "com.slack.circuit.sample.interop"
  defaultConfig {
    minSdk = 28
    // TODO update once robolectric supports 36
    targetSdk = 35
    versionCode = 1
    versionName = "1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  buildFeatures { viewBinding = true }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}

dependencies {
  debugImplementation(libs.compose.ui.tooling)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.compose.runtime.rxjava3)
  implementation(libs.compose.material.icons)
  implementation(libs.compose.material.material3)
  implementation(libs.compose.ui)
  implementation(libs.coroutines.rxjava)
  implementation(libs.molecule.runtime)
  implementation(libs.rxjava)
  implementation(libs.windowSizeClass)
  implementation(projects.circuitFoundation)

  testImplementation(libs.compose.ui.testing.junit)
  testImplementation(libs.androidx.compose.ui.testing.manifest)
  testImplementation(libs.androidx.test.espresso.core)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(projects.internalTestUtils)
}
