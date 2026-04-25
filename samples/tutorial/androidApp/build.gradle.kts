// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.plugin.compose)
  alias(libs.plugins.kotlin.plugin.parcelize)
  id("circuit.base")
}

android { namespace = "com.slack.circuit.tutorial.apk" }

dependencies {
  implementation(projects.samples.tutorial)
  implementation(projects.circuitFoundation)
  implementation(projects.backstack)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appCompat)
  implementation(libs.compose.material.material3)
  implementation(libs.compose.material.icons)
}
