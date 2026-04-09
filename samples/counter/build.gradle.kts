// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.plugin.parcelize)
}

version = "1.0.0-SNAPSHOT"

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  js(IR) {
    outputModuleName = "counterbrowser"
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = "counterbrowser"
    browser()
  }
  iosArm64()
  iosSimulatorArm64()
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitFoundation)
        api(projects.internalRuntime)
        api(libs.compose.foundation)
        api(libs.compose.material.icons)
        api(libs.compose.material.material3)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.molecule.runtime)
      }
    }
    commonTest { dependencies { implementation(libs.kotlin.test) } }
    iosMain { dependencies { api(libs.coroutines) } }

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

  swiftExport {
    moduleName = "Shared"
    flattenPackage = "com.slack.circuit.sample.counter"

    configure { freeCompilerArgs.add("-Xexpect-actual-classes") }
  }
}

android { namespace = "com.slack.circuit.sample.counter" }

androidComponents { beforeVariants { variant -> variant.androidTest.enable = false } }
