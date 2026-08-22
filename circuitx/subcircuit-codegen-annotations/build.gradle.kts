// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  id("circuit.base")
  id("circuit.publish")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.subcircuit.codegen.annotations"
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  js {
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

  @OptIn(ExperimentalKotlinGradlePluginApi::class) applyDefaultHierarchyTemplate()

  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
    }
  }
}
