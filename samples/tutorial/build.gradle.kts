// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.plugin.parcelize)
}

android {
  namespace = "com.slack.circuit.tutorial"
  defaultConfig {
    minSdk = 21
    targetSdk = 34
  }
}

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }

kotlin {
  androidTarget()
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
    val androidMain by getting {
      dependencies {
        implementation(libs.androidx.activity.ktx)
        implementation(libs.androidx.appCompat)
        implementation(libs.bundles.compose.ui)
        implementation(libs.androidx.compose.integration.activity)
        implementation(libs.material)
      }
    }
    jvmMain { dependencies { implementation(compose.desktop.currentOs) } }

    configureEach {
      @OptIn(ExperimentalKotlinGradlePluginApi::class)
      compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        if (this is KotlinJvmCompilerOptions) {
          jvmTarget.set(libs.versions.jvmTarget.map { JvmTarget.fromTarget(it) })
        }
      }
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(libs.versions.jvmTarget.map { it.toInt() })
}

compose.desktop { application { mainClass = "com.slack.circuit.tutorial.MainKt" } }
