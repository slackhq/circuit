// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
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
        api(libs.coil)
        api(libs.coil.test)
        implementation(libs.compose.runtime) // Required for the compose compiler
        implementation(compose.components.resources)
      }
    }
    androidMain { dependencies { implementation(libs.androidx.test.monitor) } }
  }
}
