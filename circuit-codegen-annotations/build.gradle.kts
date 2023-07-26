// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
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

  sourceSets {
    commonMain {
      dependencies {
        // Only here for docs linking
        compileOnly(projects.circuitFoundation)
        api(projects.circuitRuntime)
      }
    }
    val commonJvm =
      maybeCreate("commonJvm").apply {
        dependencies {
          api(libs.anvil.annotations)
          api(libs.dagger)
        }
      }
    maybeCreate("androidMain").apply { dependsOn(commonJvm) }
    maybeCreate("jvmMain").apply { dependsOn(commonJvm) }
  }
}

android { namespace = "com.slack.circuit.codegen.annotations" }
