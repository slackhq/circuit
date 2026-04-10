// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  id("circuit.base")
  id("circuit.publish")
  alias(libs.plugins.baselineprofile.consumer)
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.runtime.ui"
    compileSdk = 36
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  js(IR) {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.ui)
        api(projects.circuitRuntime)
        api(projects.circuitRuntimeScreen)
      }
    }
  }
}


baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  from(project(projects.samples.star.benchmark.path))
  filter { include("com.slack.circuit.runtime.ui.**") }
}
