// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.plugin.compose)
  alias(libs.plugins.kotlin.plugin.parcelize)
  id("circuit.base")
}

android {
  namespace = "com.slack.circuit.sample.counter.android.apk"
  defaultConfig {
    minSdk = 31 // For the dynamic m3 color schemes
  }
}

dependencies {
  implementation(projects.samples.counter.apps)
  implementation(projects.samples.counter)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.activity.compose)
  implementation(libs.compose.material.icons)
  implementation(libs.compose.ui.tooling)
}
