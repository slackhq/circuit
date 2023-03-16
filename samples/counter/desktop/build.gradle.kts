// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.compose.desktop.preview.tasks.AbstractConfigureDesktopPreviewTask

plugins {
  kotlin("jvm")
  alias(libs.plugins.compose)
  `java-library`
}

compose.desktop {
  application { mainClass = "com.slack.circuit.sample.counter.desktop.DesktopCounterCircuitKt" }
}

tasks.withType<AbstractConfigureDesktopPreviewTask>().configureEach {
  notCompatibleWithConfigurationCache("https://github.com/JetBrains/compose-jb/issues/2376")
}

dependencies {
  implementation(compose.desktop.currentOs)
  implementation(projects.samples.counter)
  implementation(projects.circuitFoundation)
}
