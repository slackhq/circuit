// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
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
        api(projects.circuitOverlay)
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(libs.compose.material.material3)
        implementation(projects.circuitFoundation)
      }
    }

    with(getByName("androidMain")) {
      dependencies {
        api(libs.androidx.compose.material.material3)
        implementation(libs.androidx.compose.accompanist.systemUi)
      }
    }

    val iosMain by getting
    val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
  }
}

android { namespace = "com.slack.circuitx.overlays" }
