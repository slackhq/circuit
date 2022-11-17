// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("plugin.parcelize")
}

android {
  namespace = "com.slack.circuit.retained.androidTest"

  defaultConfig {
    minSdk = 28
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}

dependencies {
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.coroutines)
  implementation(libs.coroutines.android)
  implementation(libs.androidx.core)
  androidTestImplementation(configurations.getByName("implementation"))
  androidTestImplementation(libs.coroutines)
  androidTestImplementation(libs.coroutines.android)
  androidTestImplementation(projects.circuitRetained)
  androidTestImplementation(libs.androidx.compose.integration.activity)
  androidTestImplementation(libs.androidx.compose.ui.testing.junit)
  androidTestImplementation(libs.androidx.compose.foundation)
  androidTestImplementation(libs.androidx.compose.ui.ui)
  androidTestImplementation(libs.androidx.compose.material.material)
  androidTestImplementation(libs.leakcanary.android.instrumentation)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.truth)
}
