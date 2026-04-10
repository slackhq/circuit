// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.nativecoroutines)
  alias(libs.plugins.kotlin.plugin.parcelize)
  id("circuit.base")
}

version = "1.0.0-SNAPSHOT"

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.sample.counter"
    compileSdk = 36
  }
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
  listOf(iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework { baseName = "CounterKt" }
  }
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
    val iosMain by sourceSets.getting { dependencies { api(libs.coroutines) } }
    val iosSimulatorArm64Main by sourceSets.getting

    configureEach { languageSettings.optIn("kotlin.experimental.ExperimentalObjCName") }
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
}

