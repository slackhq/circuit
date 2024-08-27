// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.mosaic)
  // TODO this isn't supported anymore
  //  w: 'application' (also applies 'java' plugin) Gradle plugin is not compatible with
  //  'org.jetbrains.kotlin.multiplatform' plugin.
  //  Consider adding a new subproject with 'application' plugin where the KMP project is added as a
  //  dependency.
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

  targets.withType<KotlinJvmTarget> {
    // Needed for 'application' plugin.
    withJava()
  }
}

mosaic { kotlinCompilerPlugin.set(libs.compose.compiler.map { it.toString() }) }
