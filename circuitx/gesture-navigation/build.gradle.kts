// Copyright (C) 2023 Slack Technologies, LLC
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
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js {
    moduleName = property("POM_ARTIFACT_ID").toString()
    nodejs()
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(projects.circuitFoundation)
      }
    }

    val androidMain by getting {
      dependencies {
        api(libs.compose.material.material3)
        implementation(libs.compose.uiUtil)
        implementation(libs.androidx.activity.compose)
      }
    }

    val iosMain by getting { dependencies { api(libs.compose.material.material) } }
  }
}

android { namespace = "com.slack.circuitx.gesturenavigation" }
