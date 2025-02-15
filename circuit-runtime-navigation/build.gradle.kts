// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosX64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosX64()
  tvosSimulatorArm64()
  macosX64()
  macosArm64()
  linuxArm64()
  linuxX64()
  mingwX64()
  js(IR) {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    group("browserCommon") {
      withJs()
      withWasmJs()
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.runtime.saveable)
      }
    }
    get("browserCommonMain").dependsOn(commonMain.get())
    get("browserCommonTest").dependsOn(commonTest.get())
    commonTest { dependencies { implementation(libs.kotlin.test) } }
  }

  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
    }
  }
}

android { namespace = "com.slack.circuit.runtime.navigation" }

androidComponents { beforeVariants { variant -> variant.androidTest.enable = false } }

baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  @Suppress("DEPRECATION") // https://issuetracker.google.com/issues/379030055
  from(projects.samples.star.benchmark.dependencyProject)
  filter { include("com.slack.circuit.runtime.navigation.**") }
}
