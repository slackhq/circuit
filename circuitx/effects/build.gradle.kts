// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  `java-test-fixtures`
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  macosX64()
  macosArm64()
  js(IR) {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    moduleName = property("POM_ARTIFACT_ID").toString()
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

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        implementation(projects.circuitFoundation)
        implementation(projects.circuitRetained)
        implementation(projects.circuitRuntime)
        implementation(projects.circuitRuntimeScreen)
        implementation(projects.backstack)
      }
    }
    commonTest {
      dependencies {
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(projects.circuitTest)
      }
    }
    val androidUnitTest by getting {
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
    // We use a common folder instead of a common source set because there is no commonizer
    // which exposes the browser APIs across these two targets.
    jsTest { kotlin.srcDir("src/browserTest/kotlin") }
    wasmJsTest { kotlin.srcDir("src/browserTest/kotlin") }
  }
  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
    }
  }
}

android {
  namespace = "com.slack.circuitx.sideeffects"

  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}
