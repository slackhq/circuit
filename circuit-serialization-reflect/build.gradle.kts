// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("jvm")
  alias(libs.plugins.kotlin.plugin.serialization)
  id("circuit.base")
  id("circuit.publish")
}

dependencies {
  api(projects.circuitSerialization)

  testImplementation(libs.kotlin.test)
}
