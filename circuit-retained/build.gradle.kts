// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  `java-test-fixtures`
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

    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }

    val jvmTest by getting { dependsOn(commonJvmTest) }

    // TODO export this in Android too when it's supported in kotlin projects
    val jvmMain by getting { dependencies.add("testFixturesApi", projects.circuitTest) }

    val androidInstrumentedTest by getting {
      dependsOn(commonJvmTest)
      dependencies {
        implementation(libs.coroutines)
        implementation(libs.coroutines.android)
        implementation(projects.circuitRetained)
        implementation(libs.androidx.compose.integration.activity)
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.ui.ui)
        implementation(libs.androidx.compose.material.material)
        implementation(libs.leakcanary.android.instrumentation)
        implementation(libs.junit)
        implementation(libs.truth)
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
