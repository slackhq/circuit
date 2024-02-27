import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.compose)
}

kotlin {
  // region KMP Targets
  androidTarget()
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js(IR) {
    moduleName = "internal-test-utils"
    browser()
  }
  if (hasProperty("enableWasm")) {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
      moduleName = property("POM_ARTIFACT_ID").toString()
      browser()
    }
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.foundation)
        api(libs.kotlinx.immutable)
        api(libs.coroutines)
        api(projects.backstack)
        api(projects.circuitFoundation)
        api(libs.compose.ui)
      }
    }
  }

  targets.configureEach {
    compilations.configureEach {
      compilerOptions.configure {
        freeCompilerArgs.addAll(
          "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
          "-Xexpect-actual-classes", // used for Parcelize in tests
        )
      }
    }
  }
}

android { namespace = "com.slack.circuit.internal.test" }
