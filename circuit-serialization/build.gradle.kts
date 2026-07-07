// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.compose)
  id("circuit.base")
  id("circuit.publish")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.serialization"
    compileSdk = 36
    withHostTest {
      isIncludeAndroidResources = true
    }
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosSimulatorArm64()
  macosArm64()
  linuxArm64()
  linuxX64()
  mingwX64()
  js {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("commonJvm") {
        withAndroid()
        withJvm()
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitRuntimeScreen)
        api(libs.kotlinx.serialization.core)
        api(libs.savedstate)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }
    getByName("androidHostTest") {
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
  }
}
