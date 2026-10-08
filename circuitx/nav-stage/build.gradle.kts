// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.emulatorWtf)
  id("circuit.base")
  id("circuit.publish")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuitx.navstage"
    withHostTest { isIncludeAndroidResources = true }
    withDeviceTest { androidResources { enable = true } }
  }
  jvm { testRuns["test"].executionTask.configure { enabled = false } }
  iosArm64()
  iosSimulatorArm64()
  js(IR) {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser { testTask { enabled = false } }
    binaries.executable()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser { testTask { enabled = false } }
    binaries.executable()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class) applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(projects.circuitFoundation)
        api(projects.circuitSharedElements)
        implementation(libs.compose.foundation)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.compose.ui.test)
        implementation(libs.kotlin.test)
        implementation(projects.circuitTest)
        implementation(projects.internalTestUtils)
      }
    }

    getByName("androidHostTest") {
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
        implementation(libs.androidx.activity.compose)
      }
    }

    getByName("androidDeviceTest") {
      dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.ui.testing.manifest)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.junit)
        implementation(libs.kotlin.test)
      }
    }
  }
}
