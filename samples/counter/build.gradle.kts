// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.library)
  alias(libs.plugins.skie)
}

version = "1.0.0-SNAPSHOT"

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  js {
    moduleName = "counterbrowser"
    nodejs()
  }
  listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework { baseName = "counter" }
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitFoundation)
        api(libs.compose.foundation)
        api(libs.compose.material.material3)
        implementation(libs.molecule.runtime)
      }
    }
    val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }
    val iosMain by sourceSets.getting { dependencies { api(libs.coroutines) } }
    val iosSimulatorArm64Main by sourceSets.getting

    configureEach { languageSettings.optIn("kotlin.experimental.ExperimentalObjCName") }
  }
}

android { namespace = "com.slack.circuit.sample.counter" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }
