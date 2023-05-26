// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("multiplatform")
  alias(libs.plugins.compose)
}

kotlin {
  // region KMP Targets
  ios()
  iosSimulatorArm64()
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitRuntimePresenter)
        api(libs.kmmviewmodel.core)
        implementation(libs.molecule.runtime)
      }
    }
  }
}
