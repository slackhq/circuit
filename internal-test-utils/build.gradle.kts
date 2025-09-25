// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

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
  macosX64()
  macosArm64()
  js(IR) {
    outputModuleName = "internal-test-utils"
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = "internal-test-utils"
    browser()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    group("browserCommon") {
      withJs()
      withWasmJs()
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.foundation)
        api(libs.coroutines)
        api(projects.backstack)
        api(projects.circuitFoundation)
        api(projects.internalRuntime)
        api(libs.compose.ui)
        implementation(libs.androidx.lifecycle.viewModel.compose.jb)
      }
    }
    get("browserCommonMain").dependsOn(commonMain.get())
    get("browserCommonTest").dependsOn(commonTest.get())
  }

  targets.configureEach {
    val isAndroidTarget = platformType == KotlinPlatformType.androidJvm
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions {
          freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xexpect-actual-classes", // used for Parcelize in tests
          )
          if (isAndroidTarget) {
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

android { namespace = "com.slack.circuit.internal.test" }
