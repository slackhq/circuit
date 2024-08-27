// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.baselineprofile)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosX64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosX64()
  tvosSimulatorArm64()
  macosX64()
  macosArm64()
  linuxArm64()
  linuxX64()
  mingwX64()
  js(IR) {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(libs.kotlinx.immutable)
        api(projects.circuitRuntimeScreen)
      }
    }
  }
}

android { namespace = "com.slack.circuit.runtime" }

androidComponents { beforeVariants { variant -> variant.androidTest.enable = false } }

baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  from(projects.samples.star.benchmark.dependencyProject)

  filter {
    // Don't include subpackages, only one star
    include("com.slack.circuit.runtime.*")
  }
}
