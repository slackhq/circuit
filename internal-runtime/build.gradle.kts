// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  id("circuit.base")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.internal.runtime"
    compileSdk = 36
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  js(IR) {
    outputModuleName = "internal-runtime"
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = "internal-runtime"
    browser()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("browserCommon") {
        withJs()
        withWasmJs()
      }
    }
  }

  sourceSets {
    configureEach { compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") } }
  }

  targets.configureEach {
    if (platformType == KotlinPlatformType.androidJvm) {
      compilations.configureEach {
        compileTaskProvider.configure {
          compilerOptions {
            freeCompilerArgs.addAll(
              "-P",
              "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.slack.circuit.internal.runtime.Parcelize",
            )
          }
        }
      }
    }
  }
}
