// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.library)
}

android { namespace = "com.slack.circuit.sample.coil.test" }

kotlin {
  androidTarget { publishLibraryVariants("release") }
  jvm()

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.junit)
        api(libs.coil3)
        api(libs.coil3.test)
        implementation(libs.compose.runtime) // Required for the compose compiler
        @OptIn(ExperimentalComposeLibrary::class) implementation(compose.components.resources)
      }
    }
    androidMain { dependencies { implementation(libs.androidx.test.monitor) } }
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions { optIn.addAll("coil3.annotation.ExperimentalCoilApi") }
}
