// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.compose)
  id("circuit.base")
  id("circuit.publish")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuitx.sideeffects"
    compileSdk = 36
    withHostTest { isIncludeAndroidResources = true }
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  js(IR) {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser {
      testTask {
        useKarma {
          useChromeHeadless()
          useConfigDirectory(
            rootProject.projectDir
              .resolve("internal-test-utils")
              .resolve("karma.config.d")
              .resolve("wasm")
          )
        }
      }
    }
    binaries.executable()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class) applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitRetained)
        api(projects.circuitRuntime)
      }
    }
    androidMain { dependencies { api(libs.compose.ui) } }
    commonTest {
      dependencies {
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(projects.circuitTest)
      }
    }
    getByName("androidHostTest") {
      dependencies {
        implementation(libs.androidx.compose.ui.testing.manifest)
        implementation(libs.compose.material.material3)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.robolectric)
      }
    }
  }
  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
    }
  }
}
