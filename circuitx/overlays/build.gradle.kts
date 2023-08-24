// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

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
  js {
    moduleName = property("POM_ARTIFACT_ID").toString()
    nodejs()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class) targetHierarchy.default()

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

    val androidMain by getting {
      dependencies {
        api(libs.androidx.compose.material.material3)
        implementation(libs.androidx.compose.accompanist.systemUi)
      }
    }
  }
}

android { namespace = "com.slack.circuitx.overlays" }
