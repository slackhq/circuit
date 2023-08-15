// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.baselineprofile)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  ios()
  iosSimulatorArm64()
  js(IR) {
    moduleName = property("POM_ARTIFACT_ID").toString()
    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
      }
    }
    browser()
    nodejs()
  }
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.ui)
        api(projects.circuitRuntime)
      }
    }
  }
}

android { namespace = "com.slack.circuit.runtime.ui" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }

baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  from(projects.samples.star.benchmark.dependencyProject)
  filter { include("com.slack.circuit.runtime.ui.**") }
}
