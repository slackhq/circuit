// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("multiplatform")
  id("com.android.library")
}

kotlin {
  // region KMP Targets
  android { publishLibraryVariants("release") }
  jvm()
  listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework { baseName = "counter" }
  }
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitFoundation)
        api(libs.coroutines)
      }
    }
    maybeCreate("commonTest").apply { dependencies { implementation(libs.kotlin.test) } }
  }
}

android { namespace = "com.slack.circuit.sample.counter" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }
