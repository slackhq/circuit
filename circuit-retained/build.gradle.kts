// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  `java-test-fixtures`
  alias(libs.plugins.emulatorWtf)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js(IR) {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  if (hasProperty("enableWasm")) {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
      moduleName = property("POM_ARTIFACT_ID").toString()
      browser()
    }
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.coroutines)
      }
    }

    val androidMain by getting {
      dependencies {
        api(libs.androidx.lifecycle.viewModel.compose)
        api(libs.androidx.lifecycle.viewModel)
        api(libs.androidx.compose.runtime)
        implementation(libs.androidx.compose.ui.ui)
      }
    }

    val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }

    // Necessary because android instrumented tests cannot share a source set with jvm tests for
    // some reason
    // https://kotlinlang.slack.com/archives/C0KLZSCHF/p1695237854727929
    val commonJvmTest: KotlinDependencyHandler.() -> Unit = {
      implementation(libs.junit)
      implementation(libs.truth)
    }

    val jvmTest by getting { dependencies { commonJvmTest() } }

    // TODO export this in Android too when it's supported in kotlin projects
    val jvmMain by getting { dependencies.add("testFixturesApi", projects.circuitTest) }

    val androidInstrumentedTest by getting {
      dependencies {
        commonJvmTest()
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.integration.activity)
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
      compilerOptions.configure { freeCompilerArgs.add("-Xexpect-actual-classes") }
    }
  }
}

android {
  namespace = "com.slack.circuit.retained"

  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}
