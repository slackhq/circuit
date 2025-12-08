// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.atomicfu)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.baselineprofile)
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
        implementation(libs.atomicfu)
        api(libs.compose.runtime)
        api(libs.compose.ui)
        api(libs.coroutines)
        api(projects.circuitRuntimeScreen)
        implementation(libs.compose.runtime.saveable)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.testing.assertk)
        implementation(libs.turbine)
        implementation(projects.internalTestUtils)
      }
    }
    get("browserCommonMain").dependsOn(commonMain.get())
    get("browserCommonTest").dependsOn(commonTest.get())
    androidMain {}
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependsOn(commonTest.get())
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    jvmTest { dependsOn(commonJvmTest) }
  }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  compilerOptions {
    // Need to disable, due to 'duplicate library name' warning
    // https://youtrack.jetbrains.com/issue/KT-64115
    allWarningsAsErrors = false
  }
}

android { namespace = "com.slack.circuit.backstack" }

androidComponents { beforeVariants { variant -> variant.androidTest.enable = false } }

baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  from(project(projects.samples.star.benchmark.path))
  filter { include("com.slack.circuit.backstack.**") }
}
