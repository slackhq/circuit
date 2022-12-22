// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("multiplatform")
  id("com.vanniktech.maven.publish")
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
        api(projects.backstack)
      }
    }
    maybeCreate("androidMain").apply {
      dependencies {
        api(libs.androidx.compose.runtime)
        api(libs.androidx.compose.animation)
        api(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.integration.activity)
      }
    }
    maybeCreate("commonTest").apply {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(libs.coroutines.test)
      }
    }
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    maybeCreate("jvmTest").apply { dependsOn(commonJvmTest) }
    maybeCreate("androidTest").apply {
      dependsOn(commonJvmTest)
      dependencies { implementation(libs.robolectric) }
    }
  }
}

tasks
  .withType<KotlinCompile>()
  .matching { it.name.contains("test", ignoreCase = true) }
  .configureEach {
    kotlinOptions {
      @Suppress("SuspiciousCollectionReassignment")
      freeCompilerArgs += listOf("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
  }

android { namespace = "com.slack.circuit.core" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }
