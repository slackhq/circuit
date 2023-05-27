// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("multiplatform")
  alias(libs.plugins.compose)
}

kotlin {
  // region KMP Targets
  val iosArm64 = iosArm64()
  val iosX64 = iosX64()
  val iosSimulatorArm64 = iosSimulatorArm64()
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

  listOf(
    iosArm64, iosX64, iosSimulatorArm64
  ).forEach {
    it.compilations.getByName("main") {
      cinterops.create("CircuitRuntimeObjC") {
        includeDirs("$projectDir/../CircuitRuntimeObjC")
      }
    }
  }
}
