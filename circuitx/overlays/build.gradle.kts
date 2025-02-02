// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
  macosX64()
  macosArm64()
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
        api(projects.circuitOverlay)
        implementation(projects.circuitRuntime)
        api(projects.circuitRuntimeScreen)
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(libs.compose.material.material3)
        implementation(projects.circuitFoundation)
      }
    }

    androidMain { dependencies { api(libs.androidx.compose.material.material3) } }
    // We use a common folder instead of a common source set because there is no commonizer
    // which exposes the browser APIs across these two targets.
    jsMain { kotlin.srcDir("src/browserMain/kotlin") }
    wasmJsMain { kotlin.srcDir("src/browserMain/kotlin") }
  }
}

android { namespace = "com.slack.circuitx.overlays" }
