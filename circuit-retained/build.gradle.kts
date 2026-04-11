// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  id("circuit.base")
  id("circuit.publish")
  alias(libs.plugins.emulatorWtf) apply false
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.retained"
    compileSdk = 36
    androidResources { enable = true }
    withDeviceTest {}
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosSimulatorArm64()
  macosArm64()
  linuxArm64()
  linuxX64()
  mingwX64()
  js(IR) {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("shared") {
        withAndroid()
        withJvm()
        withIos()
        withMacos()
        withWasmJs()
        withJs()
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.compose.runtime.saveable)
        api(libs.coroutines)
      }
    }

    val sharedMain = maybeCreate("sharedMain").apply {
      // ViewModel doesn't have artifacts for linux, tvOS, watchOS, or Windows
      dependencies {
        implementation(libs.lifecycle.runtime.compose)
        implementation(libs.lifecycle.viewModel.compose)
      }
    }
    iosMain { dependsOn(sharedMain) }

    commonTest { dependencies { implementation(libs.kotlin.test) } }

    // Necessary because android instrumented tests cannot share a source set with jvm tests for
    // some reason
    // https://kotlinlang.slack.com/archives/C0KLZSCHF/p1695237854727929
    val commonJvmTest: KotlinDependencyHandler.() -> Unit = {
      implementation(libs.junit)
      implementation(libs.truth)
    }

    jvmTest { dependencies { commonJvmTest() } }

    getByName("androidDeviceTest") {
      dependencies {
        commonJvmTest()
        implementation(libs.androidx.activity.compose)
        implementation(libs.compose.material.material)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.coroutines)
        implementation(libs.coroutines.android)
        implementation(libs.leakcanary.android.instrumentation)
        implementation(projects.circuitRetained)
      }
    }
  }

  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
      if (compilationName == "releaseAndroidTest") {
        compileTaskProvider.configure {
          compilerOptions { optIn.add("com.slack.circuit.retained.DelicateCircuitRetainedApi") }
        }
      }
    }
  }
}

apply(plugin = "wtf.emulator.gradle")
