// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  id(libs.plugins.baselineprofile.get().pluginId)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  ios()
  iosSimulatorArm64()
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.ui)
        api(projects.circuitRuntime)
      }
    }
  }
}

android { namespace = "com.slack.circuit.runtime.ui" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }

baselineProfile { filter { include("com.slack.circuit.runtime.ui.**") } }

dependencies { baselineProfile(projects.samples.star.benchmark) }
