// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
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
    namespace = "com.slack.circuit.sharedelements"
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  js {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
    binaries.executable()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
    binaries.executable()
  }
  // endregion
  applyDefaultHierarchyTemplate()

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions.optIn.add("com.slack.circuit.sharedelements.ExperimentalCircuitSharedElementsApi")

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.foundation)
        api(libs.compose.ui)
      }
    }
  }
}

baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  from(project(projects.samples.star.benchmark.path))
  filter { include("com.slack.circuit.sharedelements.**") }
}
