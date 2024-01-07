// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.multiplatform)
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
      }
    }
    androidMain { dependencies { implementation(libs.androidx.test.monitor) } }
  }

  compilerOptions {
    optIn.addAll(
      "coil3.annotation.ExperimentalCoilApi",
    )
  }
}
