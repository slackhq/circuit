// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.agp.kmp)
  id("circuit.base")
}

kotlin {
  android {
    namespace = "com.slack.circuit.sample.coil.test"
    compileSdk = 36
  }
  jvm()

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.junit)
        api(libs.coil)
        api(libs.coil.test)
        implementation(libs.compose.components.resources)
        implementation(libs.compose.runtime) // Required for the compose compiler
      }
    }
    androidMain { dependencies { implementation(libs.androidx.test.monitor) } }
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions { optIn.addAll("coil3.annotation.ExperimentalCoilApi") }
}
