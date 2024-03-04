// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
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
        api(projects.circuitFoundation)
      }
    }

    androidMain {
      dependencies {
        api(libs.compose.material.material3)
        implementation(libs.compose.uiUtil)
        implementation(libs.androidx.activity.compose)
        // Because guava's dependencies are a tangled mess
        implementation(libs.guava.listenablefuture)
      }
    }

    val iosMain by getting { dependencies { api(libs.compose.material.material) } }

    val androidUnitTest by getting {
      dependencies {
        implementation(projects.internalTestUtils)

        implementation(libs.robolectric)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
  }
}

android {
  namespace = "com.slack.circuitx.gesturenavigation"

  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}
