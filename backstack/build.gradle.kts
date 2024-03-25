// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.atomicfu)
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

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.atomicfu)
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
    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.testing.assertk)
        implementation(projects.internalTestUtils)
      }
    }
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    val jvmTest by getting { dependsOn(commonJvmTest) }
    // We use a common folder instead of a common source set because there is no commonizer
    // which exposes the browser APIs across these two targets.
    jsMain { kotlin.srcDir("src/browserMain/kotlin") }
    val wasmJsMain by getting { kotlin.srcDir("src/browserMain/kotlin") }
  }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  compilerOptions {
    // Need to disable, due to 'duplicate library name' warning
    // https://youtrack.jetbrains.com/issue/KT-64115
    allWarningsAsErrors = false
  }
}

android { namespace = "com.slack.circuit.backstack" }

androidComponents { beforeVariants { variant -> variant.androidTest.enable = false } }

baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  from(projects.samples.star.benchmark.dependencyProject)
  filter { include("com.slack.circuit.backstack.**") }
}
