// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.compose)
  application
}

application { mainClass.set("com.slack.circuit.sample.counter.mosaic.Main") }

version = "1.0.0-SNAPSHOT"

dependencies {
  implementation(libs.mosaic)
  implementation(libs.jline)
  implementation(projects.samples.counter)
  implementation(projects.circuitFoundation)
}
