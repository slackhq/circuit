// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mosaic)
  application
}

application { mainClass.set("com.slack.circuit.sample.counter.mosaic.jvm.Main") }

version = "1.0.0-SNAPSHOT"

kotlin {
  // region KMP Targets
  jvm()
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.samples.counter)
        implementation(projects.circuitFoundation)
      }
    }
    // TODO is there a multiplatform way to do this?
    jvmMain { dependencies { implementation(libs.jline) } }
  }

  // TODO https://youtrack.jetbrains.com/issue/KT-67636
  //  targets.withType<KotlinJvmTarget> {
  //   // Needed for 'application' plugin.
  //    withJava()
  //  }
}

mosaic { kotlinCompilerPlugin.set(libs.compose.compiler.map { it.toString() }) }
