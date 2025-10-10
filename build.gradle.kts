// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import kotlinx.validation.ExperimentalBCVApi

buildscript { dependencies { classpath(platform(libs.kotlin.plugins.bom)) } }

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.plugin.parcelize) apply false
  alias(libs.plugins.kotlin.plugin.serialization) apply false
  alias(libs.plugins.agp.application) apply false
  alias(libs.plugins.agp.library) apply false
  alias(libs.plugins.agp.test) apply false
  alias(libs.plugins.anvil) apply false
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.dependencyGuard) apply false
  alias(libs.plugins.compose) apply false
  alias(libs.plugins.kotlin.plugin.compose) apply false
  alias(libs.plugins.baselineprofile) apply false
  alias(libs.plugins.emulatorWtf) apply false
  alias(libs.plugins.binaryCompatibilityValidator)
  alias(libs.plugins.compose.hotReload) apply false
}

val ktfmtVersion = libs.versions.ktfmt.get()
val detektVersion = libs.versions.detekt.get()
val twitterDetektPlugin = libs.detektPlugins.twitterCompose

dokka {
  dokkaPublications.html {
    outputDirectory.set(rootDir.resolve("docs/api/0.x"))
    includes.from(project.layout.projectDirectory.file("README.md"))
  }
}

val jvmTargetVersion = libs.versions.jvmTarget
val publishedJvmTargetVersion = libs.versions.publishedJvmTarget

apiValidation {
  @OptIn(ExperimentalBCVApi::class)
  klib {
    enabled = true
    strictValidation = false
  }
  nonPublicMarkers +=
    setOf(
      "com.slack.circuit.runtime.InternalCircuitApi",
      "com.slack.circuit.runtime.ExperimentalCircuitApi",
      "com.slack.circuit.test.ExperimentalForInheritanceCircuitTestApi",
    )
  ignoredPackages +=
    setOf("com.slack.circuit.foundation.internal", "com.slack.circuit.runtime.internal")
  // Annoyingly this only uses simple names
  // https://github.com/Kotlin/binary-compatibility-validator/issues/16
  ignoredProjects +=
    listOf(
      "apk",
      "apps",
      "benchmark",
      "bottom-navigation",
      "circuit-codegen",
      "coil-rule",
      "counter",
      "internal-runtime",
      "internal-test-utils",
      "interop",
      "kotlin-inject",
      "mosaic",
      "star",
      "tacos",
      "tutorial",
    )
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
