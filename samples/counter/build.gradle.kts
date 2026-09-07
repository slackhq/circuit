// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.nativecoroutines)
  alias(libs.plugins.kotlin.plugin.serialization)
  id("circuit.base")
}

version = "1.0.0-SNAPSHOT"

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.sample.counter"
  }
  jvm()
  js {
    outputModuleName = "counterbrowser"
    browser()
    binaries.executable()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = "counterbrowser"
    browser()
    binaries.executable()
  }
  listOf(iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework { baseName = "CounterKt" }
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitFoundation)
        api(libs.compose.foundation)
        api(libs.compose.material.icons)
        api(libs.compose.material.material3)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.molecule.runtime)
        implementation(projects.circuitSerialization)
      }
    }
    commonTest { dependencies { implementation(libs.kotlin.test) } }
    sourceSets.maybeCreate("iosMain").apply { dependencies { api(libs.coroutines) } }
    sourceSets.maybeCreate("iosSimulatorArm64Main")

    configureEach { languageSettings.optIn("kotlin.experimental.ExperimentalObjCName") }
  }
}
