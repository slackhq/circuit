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
  android {  publishLibraryVariants("release") }
  jvm()
  ios()
  iosSimulatorArm64()
  // endregion

  sourceSets {
    commonMain { dependencies { api(libs.compose.runtime) } }
    val iosMain by getting
    val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
  }
}

android { namespace = "com.slack.circuit.runtime" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }

baselineProfile {
  // https://issuetracker.google.com/issues/282127523
  baselineProfileOutputDir = "../../src/androidMain/generated/baselineProfiles"
  filter {
    // Don't include subpackages, only one star
    include("com.slack.circuit.runtime.*")
  }
}

dependencies { baselineProfile(projects.samples.star.benchmark) }
