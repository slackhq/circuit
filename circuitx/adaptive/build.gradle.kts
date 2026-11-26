// Copyright (C) 2026 Slack Technologies, LLC
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
    namespace = "com.slack.circuitx.adaptive"
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  js {
    outputModuleName.set(property("POM_ARTIFACT_ID").toString())
    browser()
    binaries.executable()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName.set(property("POM_ARTIFACT_ID").toString())
    browser()
    binaries.executable()
  }
  // endregion
  applyDefaultHierarchyTemplate()

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions.optIn.add("com.slack.circuitx.adaptive.ExperimentalCircuitAdaptiveApi")

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.ui)
        api(projects.circuitFoundation)
        api(projects.circuitx.navigation)
        // WindowSizeClass is part of PaneWindow.
        api(libs.androidx.window.core)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3.adaptive)
        implementation(libs.compose.material3.adaptive.layout)
        implementation(projects.circuitSharedElements)
      }
    }
    jvmTest {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.compose.ui.test)
        implementation(libs.junit)
        implementation(libs.kotlin.test)
        implementation(libs.testing.assertk)
        implementation(projects.circuitTest)
      }
    }
  }
}
