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
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
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

  @OptIn(ExperimentalKotlinGradlePluginApi::class) applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(projects.circuitFoundation)
        // For CupertinoGestureNavigationDecoration
        implementation(libs.compose.material.material)
        implementation(libs.compose.ui.backhandler)
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
        implementation(projects.internalTestUtils)

        implementation(libs.robolectric)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }

    iosTest {
      dependencies {
        implementation(libs.compose.ui.test)
        implementation(libs.kotlin.test)
        implementation(projects.circuitTest)
        implementation(projects.internalTestUtils)
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
