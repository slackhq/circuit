// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.plugin.compose)
  alias(libs.plugins.kotlin.plugin.parcelize)
  id("circuit.base")
}

android { namespace = "com.slack.circuit.sample.navigation.app" }

dependencies {
  implementation(projects.samples.bottomNavigation)
  implementation(projects.circuitFoundation)
  implementation(projects.circuitx.android)
  implementation(projects.circuitx.gestureNavigation)
  implementation(projects.circuitx.navigation)
  implementation(projects.internalRuntime)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appCompat)
  implementation(libs.compose.foundation)
  implementation(libs.compose.material.material3)
  implementation(libs.compose.ui.tooling.preview)
}
