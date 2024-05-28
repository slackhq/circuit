// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.plugin.compose)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.mavenPublish)
}

android { namespace = "com.slack.circuitx.android" }

dependencies {
  implementation(libs.androidx.annotation)
  api(projects.circuitRuntime)
}
