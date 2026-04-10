// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  id("circuit.base")
  id("circuit.publish")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuitx.navigation"
    compileSdk = 36
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
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

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("commonJvm") {
        withAndroid()
        withJvm()
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(projects.circuitFoundation)
        implementation(libs.compose.navigationevent)
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
    maybeCreate("commonJvmTest").apply {
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
      }
    }
    jvmTest {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.compose.ui.testing.junit)
      }
    }
  }
}
