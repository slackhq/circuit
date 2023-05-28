// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
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
        api(libs.coroutines)
        implementation(libs.uuid)
        implementation(libs.compose.runtime.saveable)
      }
    }
    maybeCreate("androidMain").apply {
      dependencies {
        implementation(libs.androidx.lifecycle.viewModel.compose)
        api(libs.androidx.lifecycle.viewModel)
        api(libs.androidx.compose.runtime)
      }
    }
    maybeCreate("commonTest").apply { dependencies { implementation(libs.kotlin.test) } }
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    maybeCreate("jvmTest").apply { dependsOn(commonJvmTest) }
    val iosMain by getting
    val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
  }
}

android { namespace = "com.slack.circuit.backstack" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }

baselineProfile {
  // https://issuetracker.google.com/issues/282127523
  baselineProfileOutputDir = "../../src/androidMain/generated/baselineProfiles"
  filter { include("com.slack.circuit.backstack.**") }
}

dependencies { baselineProfile(projects.samples.star.benchmark) }
