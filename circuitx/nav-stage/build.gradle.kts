// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  id("circuit.base")
  id("circuit.publish")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuitx.navstage"
    compileSdk = 36
  }
  jvm { testRuns["test"].executionTask.configure { enabled = false } }
  iosArm64()
  iosSimulatorArm64()
  js(IR) {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser { testTask { enabled = false } }
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser { testTask { enabled = false } }
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class) applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(projects.circuitFoundation)
        implementation(libs.compose.foundation)
        implementation(libs.windowSizeClass)
      }
    }
  }
}
