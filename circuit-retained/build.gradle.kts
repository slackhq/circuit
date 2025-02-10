// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.emulatorWtf)
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
        api(libs.coroutines)
      }
    }

    get("browserCommonMain").dependsOn(commonMain.get())
    get("browserCommonTest").dependsOn(commonTest.get())

    androidMain {
      dependencies {
        api(libs.androidx.lifecycle.viewModel.compose)
        api(libs.androidx.lifecycle.viewModel)
        api(libs.androidx.compose.runtime)
        implementation(libs.androidx.compose.ui.ui)
      }
    }

    commonTest { dependencies { implementation(libs.kotlin.test) } }

    // Necessary because android instrumented tests cannot share a source set with jvm tests for
    // some reason
    // https://kotlinlang.slack.com/archives/C0KLZSCHF/p1695237854727929
    val commonJvmTest: KotlinDependencyHandler.() -> Unit = {
      implementation(libs.junit)
      implementation(libs.truth)
    }

    jvmTest { dependencies { commonJvmTest() } }

    // TODO export this in Android too when it's supported in kotlin projects
    jvmMain { dependencies.add("testFixturesApi", projects.circuitTest) }

    androidInstrumentedTest {
      dependencies {
        commonJvmTest()
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.material.material)
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.ui)
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

android {
  namespace = "com.slack.circuit.retained"

  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}
