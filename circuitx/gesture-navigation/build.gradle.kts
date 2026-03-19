// Copyright (C) 2023 Slack Technologies, LLC
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
        implementation(libs.compose.navigationevent)
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

    androidMain {
      dependencies {
        api(libs.compose.material.material3)
        implementation(libs.compose.ui.util)
        implementation(libs.androidx.activity.compose)
      }
    }

    androidUnitTest {
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
  }
}

android {
  namespace = "com.slack.circuitx.gesturenavigation"

  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}
