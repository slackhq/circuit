// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.kotlin.plugin.parcelize)
  id("circuit.base")
}

kotlin {
  android {
    namespace = "com.slack.circuit.tutorial"
    compileSdk = 36
  }
  jvm {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    mainRun { mainClass.set("com.slack.circuit.tutorial.MainKt") }
  }
  jvmToolchain(libs.versions.jdk.get().toInt())

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.compose.foundation)
        implementation(libs.compose.material.material3)
        implementation(libs.compose.material.icons)
        implementation(libs.compose.ui.tooling.preview)
        implementation(projects.circuitFoundation)
      }
    }
    androidMain {}
    jvmMain { dependencies { implementation(compose.desktop.currentOs) } }

    configureEach {
      compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
      }
    }
  }
}

compose.desktop { application { mainClass = "com.slack.circuit.tutorial.MainKt" } }
