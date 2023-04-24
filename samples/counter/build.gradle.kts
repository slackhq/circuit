// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("multiplatform")
  alias(libs.plugins.compose)
  kotlin("native.cocoapods")
  id("com.android.library")
}

version = "1.0.0-SNAPSHOT"

kotlin {
  // region KMP Targets
  android { publishLibraryVariants("release") }
  jvm()
  ios()
  iosSimulatorArm64()
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
    extraSpecAttributes["resources"] = "['src/commonMain/resources/**', 'src/iosMain/resources/**']"
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
