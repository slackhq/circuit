// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
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

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    group("browserCommon") {
      withJs()
      withWasmJs()
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitOverlay)
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(libs.compose.material.material3)
        implementation(libs.compose.ui.backhandler)
        implementation(projects.circuitFoundation)
      }
    }

    get("browserCommonMain").dependsOn(commonMain.get())
    get("browserCommonTest").dependsOn(commonTest.get())

    androidMain { dependencies { api(libs.androidx.compose.material.material3) } }
  }
}

android { namespace = "com.slack.circuitx.overlays" }
