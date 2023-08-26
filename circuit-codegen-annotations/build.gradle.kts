// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.mavenPublish)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  // Anvil/Dagger does not support iOS targets
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class) targetHierarchy.default()

  sourceSets {
    commonMain {
      dependencies {
        // Only here for docs linking
        compileOnly(projects.circuitFoundation)
        api(projects.circuitRuntimeScreen)
      }
    }
    val commonJvm =
      maybeCreate("commonJvm").apply {
        dependencies {
          api(libs.anvil.annotations)
          api(libs.dagger)
        }
      }
    val androidMain by getting { dependsOn(commonJvm) }
    val jvmMain by getting { dependsOn(commonJvm) }
  }
}

android { namespace = "com.slack.circuit.codegen.annotations" }
