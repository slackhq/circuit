// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  kotlin("native.cocoapods")
  alias(libs.plugins.agp.library)
}

version = "1.0.0-SNAPSHOT"

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js {
    moduleName = "counterbrowser"
    nodejs()
  }
  // TODO regular frameworks are not yet supported
  //  listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
  //    it.binaries.framework {
  //      baseName = "counter"
  //      isStatic = true
  //    }
  //  }

  cocoapods {
    summary = "Counter presenter implementation"
    homepage = "None"
    ios.deploymentTarget = "14.1"
    podfile = project.file("apps/Podfile")
    framework {
      baseName = "counter"
      isStatic = true
    }
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
