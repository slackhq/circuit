// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.roborazzi)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js {
    moduleName = property("POM_ARTIFACT_ID").toString()
    nodejs()
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitOverlay)
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(libs.compose.material.material3)
        implementation(projects.circuitFoundation)
      }
    }

    val androidMain by getting {
      dependencies {
        api(libs.androidx.compose.material.material3)
        implementation(libs.androidx.compose.accompanist.systemUi)
      }
    }

    val androidUnitTest by getting {
      dependencies {
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
        implementation(libs.androidx.loader)
        implementation(libs.leakcanary.android.instrumentation)
        implementation(libs.robolectric)
        implementation(libs.roborazzi)
        implementation(libs.roborazzi.compose)
        implementation(libs.roborazzi.rules)
        implementation(libs.testing.espresso.core)
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.roborazzi.compose.desktop.get().toString()) {
          exclude("org.jetbrains.compose.ui", "ui-test-junit4-desktop")
          exclude("org.jetbrains.compose.ui", "ui-graphics-desktop")
        }
        implementation(libs.compose.test.junit4)
        implementation(libs.kotlin.test)
      }
      // Roborazzi Desktop support uses Context Receivers
      @OptIn(ExperimentalKotlinGradlePluginApi::class)
      compilerOptions { freeCompilerArgs.add("-Xcontext-receivers") }
    }
  }
}

android {
  namespace = "com.slack.circuitx.overlays"

  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testApplicationId = "com.slack.circuitx.overlays.androidTest"
  }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}
