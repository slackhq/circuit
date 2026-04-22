// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
buildscript { dependencies { classpath(platform(libs.kotlin.plugins.bom)) } }

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.plugin.parcelize) apply false
  alias(libs.plugins.kotlin.plugin.serialization) apply false
  alias(libs.plugins.agp.application) apply false
  alias(libs.plugins.agp.kmp) apply false
  alias(libs.plugins.agp.library) apply false
  alias(libs.plugins.agp.test) apply false
  alias(libs.plugins.anvil) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.dependencyGuard) apply false
  alias(libs.plugins.compose) apply false
  alias(libs.plugins.kotlin.plugin.compose) apply false
  alias(libs.plugins.baselineprofile) apply false
  alias(libs.plugins.baselineprofile.consumer) apply false
  alias(libs.plugins.emulatorWtf) apply false
  id("circuit.base") apply false
  id("circuit.spotless")
}

dokka {
  dokkaPublications.html {
    outputDirectory.set(rootDir.resolve("docs/api/0.x"))
    includes.from(project.layout.projectDirectory.file("README.md"))
  }
}

develocity {
  buildScan {
    // Has to be configured here because settings gradle objects (i.e. if this were in
    // settings.gradle.kts) can't be captured in buildScanPublished {}
    val isQuiet = gradle.startParameter.logLevel == LogLevel.QUIET
    buildScanPublished {
      if (isQuiet) {
        // Normally GE prints this for us already, but when running with --quiet on CI
        // Gradle actually annoyingly doesn't print this, so we replicate it.
        // https://github.com/gradle/gradle/issues/5043#issuecomment-655505445
        println("Publishing build scan...\n$buildScanUri")
      }
    }
  }
}

// Dokka aggregating deps
dependencies {
  dokka(projects.backstack)
  dokka(projects.circuitCodegen)
  dokka(projects.circuitCodegenAnnotations)
  dokka(projects.circuitFoundation)
  dokka(projects.circuitOverlay)
  dokka(projects.circuitRetained)
  dokka(projects.circuitRuntime)
  dokka(projects.circuitRuntimePresenter)
  dokka(projects.circuitRuntimeScreen)
  dokka(projects.circuitRuntimeUi)
  dokka(projects.circuitTest)
  dokka(projects.circuitx.android)
  dokka(projects.circuitx.effects)
  dokka(projects.circuitx.gestureNavigation)
  dokka(projects.circuitx.overlays)
}

val circuitCi: TaskProvider<Task> =
  tasks.register("circuitCi") {
    group = "CI"
    description = "Aggregates multiple verification tasks for CI."
    dependsOn(
      ":samples:star:androidApp:assembleDebug",
      ":samples:star:jvmJar",
      ":samples:bottom-navigation:androidApp:assembleDebug",
      ":samples:bottom-navigation:jvmJar",
    )
  }

afterEvaluate {
  circuitCi.configure {
    allprojects {
      fun Task.findAndDependOn(name: String) {
        tasks.findByPath(name)?.let { dependsOn(it) }
      }
      findAndDependOn("check")
      findAndDependOn("assembleAndroidTest")
    }
  }
}
