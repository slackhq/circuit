// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
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
    outputModuleName.set(property("POM_ARTIFACT_ID").toString())
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName.set(property("POM_ARTIFACT_ID").toString())
    browser()
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(projects.circuitFoundation)
        implementation(libs.compose.ui.backhandler)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.lifecycle.runtime.compose)
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.testing.assertk)
        implementation(libs.turbine)
        implementation(projects.circuitTest)
        implementation(projects.internalTestUtils)
      }
    }

    androidMain { dependencies { api(projects.circuitx.android) } }
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependsOn(commonTest.get())
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    jvmTest {
      dependsOn(commonJvmTest)
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.compose.ui.testing.junit)
      }
    }
  }
}

android { namespace = "com.slack.circuitx.navigation" }
