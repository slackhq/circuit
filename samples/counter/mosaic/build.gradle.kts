// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
  kotlin("jvm")
  alias(libs.plugins.mosaic)
  `application`
  `java-library`
}

application { mainClass.set("com.slack.circuit.sample.counter.mosaic.MosaicCounterCircuitKt") }

dependencies {
  implementation(projects.samples.counter)
  implementation(projects.circuit)
  implementation(libs.clikt)
  implementation(libs.jline)
  add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
}
