// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("plugin.parcelize")
}

android {
  namespace = "com.slack.circuit.sample.interop"
  defaultConfig {
    minSdk = 33
    targetSdk = 33
    versionCode = 1
    versionName = "1"
  }
  buildFeatures { viewBinding = true }
}

dependencies {
  implementation(projects.circuitFoundation)
  // Reuse the counter presenter logic for this sample
  implementation(projects.samples.counter)
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
  implementation(libs.kotlinx.immutable)
  implementation(libs.rxjava)
  implementation(libs.coroutines.rxjava)
  implementation(libs.molecule.runtime)
  implementation(libs.androidx.compose.runtime.rxjava3)
}
