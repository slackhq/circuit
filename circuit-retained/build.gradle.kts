// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("com.android.library")
  kotlin("multiplatform")
  id("com.vanniktech.maven.publish")
  `java-test-fixtures`
}

kotlin {
  // region KMP Targets
  android { publishLibraryVariants("release") }
  jvm()
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.coroutines)
      }
    }
    maybeCreate("androidMain").apply {
      dependencies {
        api(libs.androidx.lifecycle.viewModel.compose)
        api(libs.androidx.lifecycle.viewModel)
        api(libs.androidx.compose.runtime)
      }
    }
    maybeCreate("commonTest").apply { dependencies { implementation(libs.kotlin.test) } }
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    maybeCreate("jvmTest").apply { dependsOn(commonJvmTest) }
    // TODO export this in Android too when it's supported in kotlin projects
    maybeCreate("jvmMain").apply { dependencies.add("testFixturesApi", projects.circuitTest) }
  }
}

android { namespace = "com.slack.circuit.retained" }
