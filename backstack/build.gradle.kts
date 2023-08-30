// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

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
  js {
    moduleName = property("POM_ARTIFACT_ID").toString()
    nodejs()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class) targetHierarchy.default()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.ui)
        api(libs.coroutines)
        api(libs.kotlinx.immutable)
        api(projects.circuitRuntimeScreen)
        implementation(libs.compose.runtime.saveable)
        implementation(libs.uuid)
      }
    }
    val androidMain by getting {
      dependencies {
        implementation(libs.androidx.lifecycle.viewModel.compose)
        api(libs.androidx.lifecycle.viewModel)
        api(libs.androidx.compose.runtime)
      }
    }
    val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    val jvmTest by getting { dependsOn(commonJvmTest) }
  }
}

android { namespace = "com.slack.circuit.backstack" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }

baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  from(projects.samples.star.benchmark.dependencyProject)
  filter { include("com.slack.circuit.backstack.**") }
}
