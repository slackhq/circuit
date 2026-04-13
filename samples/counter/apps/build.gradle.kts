// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.kotlin.plugin.parcelize)
  id("circuit.base")
}

compose.desktop {
  application { mainClass = "com.slack.circuit.sample.counter.desktop.DesktopCounterCircuitKt" }
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.sample.counter.android"
    compileSdk = 36
  }
  jvm()
  iosArm64()
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = "counterApp"
    browser { commonWebpackConfig { outputFileName = "counterApp.js" } }
    binaries.executable()
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(projects.samples.counter)
        api(projects.circuitFoundation)
      }
    }
    commonTest { dependencies { implementation(libs.kotlin.test) } }
    jvmMain { dependencies { implementation(compose.desktop.currentOs) } }
    androidMain {}
    wasmJsMain {
      dependencies {
        implementation(libs.compose.components.resources)
        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
      }
    }
  }
}
