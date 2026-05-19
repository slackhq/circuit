// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.atomicfu)
  alias(libs.plugins.compose)
  id("circuit.base")
  id("circuit.publish")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.backstack"
    compileSdk = 36
    withHostTest { isReturnDefaultValues = true }
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
            rootProject.isolated.projectDirectory
              .dir("internal-test-utils/karma.config.d/wasm")
              .asFile
          )
        }
      }
    }
    binaries.executable()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("commonJvm") {
        withJvm()
        withAndroid()
      }
    }
    common {
      group("browserCommon") {
        withJs()
        withWasmJs()
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.atomicfu)
        api(libs.compose.runtime)
        api(libs.compose.ui)
        api(libs.coroutines)
        api(projects.circuitRuntimeNavigation)
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
    maybeCreate("commonJvmTest").apply {
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
      }
    }
    getByName("androidHostTest") {
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
  }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  compilerOptions {
    // Need to disable, due to 'duplicate library name' warning
    // https://youtrack.jetbrains.com/issue/KT-64115
    allWarningsAsErrors = false
  }
}
