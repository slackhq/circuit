// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  `java-test-fixtures`
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js {
    moduleName = property("POM_ARTIFACT_ID").toString()
    nodejs()
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        implementation(projects.circuitFoundation)
      }
    }
    val androidInstrumentedTest by getting {
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
        implementation(libs.coroutines)
        implementation(libs.coroutines.android)
        implementation(projects.circuitTest)
        implementation(projects.circuitx.sideeffects)
        implementation(libs.androidx.compose.integration.activity)
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.ui.ui)
        implementation(libs.leakcanary.android.instrumentation)
      }
    }
  }
}

android {
  namespace = "com.slack.circuitx.sideeffects"

  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}
