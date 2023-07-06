// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

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

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.samples.counter)
        implementation(projects.circuitFoundation)
      }
    }
    // TODO is there a multiplatform way to do this?
    maybeCreate("jvmMain").apply { dependencies { implementation(libs.jline) } }
  }

  targets.withType<KotlinJvmTarget> {
    // Needed for 'application' plugin.
    withJava()
  }
}

configurations
  .matching { it.name == "kotlinCompilerPluginClasspathJvmMain" }
  .configureEach {
    // Mosaic imposes its compose compiler version, so we need to exclude whichever one
    // we're not using
    val (group, artifact) =
      if (property("circuit.forceAndroidXComposeCompiler").toString().toBoolean()) {
        // JB version
        libs.compose.compilerJb.get().toString().split(":")
      } else {
        // Google version
        libs.androidx.compose.compiler.get().toString().split(":")
      }
    exclude(group, artifact)
  }
