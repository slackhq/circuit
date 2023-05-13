// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("com.android.library")
  kotlin("multiplatform")
  alias(libs.plugins.compose)
  id("com.vanniktech.maven.publish")
  alias(libs.plugins.baselineprofile)
}

kotlin {
  // region KMP Targets
  android { publishLibraryVariants("release") }
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

baselineProfile {
  // https://issuetracker.google.com/issues/282127523
  baselineProfileOutputDir = "src/androidMain/generated/baselineProfiles"
  filter {
    include("com.slack.circuit.runtime.ui")
    include("com.slack.circuit.runtime.ui.**")
  }
}

dependencies { baselineProfile(projects.samples.star.benchmark) }
