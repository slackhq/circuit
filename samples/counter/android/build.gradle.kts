// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("plugin.parcelize")
}

android {
  namespace = "com.slack.circuit.sample.counter.android"
  defaultConfig {
    compileSdk = 33
    minSdk = 33
    targetSdk = 33
  }
}

dependencies {
  implementation(projects.samples.counter)
  implementation(projects.circuit)
  implementation(libs.androidx.appCompat)
  implementation(libs.bundles.compose.ui)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.compose.integration.materialThemeAdapter)
  implementation(libs.androidx.compose.material.icons)
  implementation(libs.androidx.compose.material.iconsExtended)
  implementation(libs.androidx.compose.accompanist.systemUi)
}
