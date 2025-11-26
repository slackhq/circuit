// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
dependencyResolutionManagement {
  versionCatalogs {
    if (System.getenv("DEP_OVERRIDES") == "true" || System.getProperty("DEP_OVERRIDES") == "true") {
      // Source both env vars and system props for overrides. System props take precedence.
      val envOverrides = System.getenv().filterKeys { it.startsWith("DEP_OVERRIDE_") }
      val systemPropOverrides =
        System.getProperties()
          .filterKeys { it.toString().startsWith("DEP_OVERRIDE_") }
          .entries
          .associate { (k, v) -> k.toString() to v.toString() }
      val overrides =
        (envOverrides + systemPropOverrides).mapKeys { (key, _) ->
          // Case-sensitive, don't adjust it after removing the prefix!
          key.removePrefix("DEP_OVERRIDE_")
        }
      configureEach {
        val catalog = this
        for ((catalogKey, value) in overrides) {
          println("Overriding $catalogKey with $value in catalog '${catalog.name}'")
          catalog.version(catalogKey, value)
        }
      }
    }
  }

  // Non-delegate APIs are annoyingly not public so we have to use withGroovyBuilder
  fun hasProperty(key: String): Boolean {
    return settings.withGroovyBuilder { "hasProperty"(key) as Boolean }
  }

  fun findProperty(key: String): String? {
    return if (hasProperty(key)) {
      settings.withGroovyBuilder { "getProperty"(key) as String }
    } else {
      null
    }
  }

  repositories {
    // Repos are declared roughly in order of likely to hit.

    // Snapshots/local go first in order to pre-empty other repos that may contain unscrupulous
    // snapshots.
    if (hasProperty("circuit.config.enableSnapshots")) {
      maven(findProperty("circuit.mavenUrls.snapshots.sonatype")!!) {
        name = "snapshots-maven-central"
        mavenContent { snapshotsOnly() }
      }
      maven(findProperty("circuit.mavenUrls.snapshots.sonatypes01")!!) {
        name = "snapshots-maven-central-s01"
        mavenContent { snapshotsOnly() }
      }
      maven(findProperty("circuit.mavenUrls.snapshots.androidx")!!) {
        name = "snapshots-androidx"
        mavenContent { snapshotsOnly() }
        content { includeGroupByRegex("androidx.*") }
      }
    }

    if (hasProperty("circuit.config.enableMavenLocal")) {
      mavenLocal()
    }

    mavenCentral()

    google()

    // Kotlin dev repository, useful for testing against Kotlin dev builds.
    // Usually only tested on CI shadow jobs
    // https://kotlinlang.slack.com/archives/C0KLZSCHF/p1616514468003200?thread_ts=1616509748.001400&cid=C0KLZSCHF
    maven(findProperty("circuit.mavenUrls.kotlinDev")!!) {
      name = "Kotlin-Bootstrap"
      content {
        // this repository *only* contains Kotlin artifacts (don't try others here)
        includeGroupByRegex("org\\.jetbrains.*")
      }
    }

    exclusiveContent {
      forRepository {
        // For R8/D8 releases
        maven("https://storage.googleapis.com/r8-releases/raw")
      }
      filter { includeModule("com.android.tools", "r8") }
    }

    // JB Compose Repo
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") { name = "Compose-JB" }

    // emulator.wtf
    maven(url = "https://maven.emulator.wtf/releases/") { content { includeGroup("wtf.emulator") } }
  }
}

pluginManagement {
  // Non-delegate APIs are annoyingly not public so we have to use withGroovyBuilder
  fun hasProperty(key: String): Boolean {
    return settings.withGroovyBuilder { "hasProperty"(key) as Boolean }
  }

  fun findProperty(key: String): String? {
    return if (hasProperty(key)) {
      settings.withGroovyBuilder { "getProperty"(key) as String }
    } else {
      null
    }
  }

  repositories {
    // Repos are declared roughly in order of likely to hit.

    // Snapshots/local go first in order to pre-empty other repos that may contain unscrupulous
    // snapshots.
    if (hasProperty("circuit.config.enableSnapshots")) {
      maven(findProperty("circuit.mavenUrls.snapshots.sonatype")!!) {
        name = "snapshots-maven-central"
        mavenContent { snapshotsOnly() }
      }
      maven(findProperty("circuit.mavenUrls.snapshots.sonatypes01")!!) {
        name = "snapshots-maven-central-s01"
        mavenContent { snapshotsOnly() }
      }
      maven(findProperty("circuit.mavenUrls.snapshots.androidx")!!) {
        name = "snapshots-androidx"
        mavenContent { snapshotsOnly() }
        content { includeGroupByRegex("androidx.*") }
      }
    }

    if (hasProperty("circuit.config.enableMavenLocal")) {
      mavenLocal()
    }

    mavenCentral()

    google()

    // Kotlin dev repository, useful for testing against Kotlin dev builds.
    // Usually only tested on CI shadow jobs
    // https://kotlinlang.slack.com/archives/C0KLZSCHF/p1616514468003200?thread_ts=1616509748.001400&cid=C0KLZSCHF
    maven(findProperty("circuit.mavenUrls.kotlinDev")!!) {
      name = "Kotlin-Bootstrap"
      content {
        // this repository *only* contains Kotlin artifacts (don't try others here)
        includeGroupByRegex("org\\.jetbrains.*")
      }
    }

    // JB Compose Repo
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") { name = "Compose-JB" }

    // Gradle's plugin portal proxies jcenter, which we don't want. To avoid this, we specify
    // exactly which dependencies to pull from here.
    exclusiveContent {
      forRepository(::gradlePluginPortal)
      filter {
        includeModule("com.github.ben-manes", "gradle-versions-plugin")
        includeModule(
          "com.github.ben-manes.versions",
          "com.github.ben-manes.versions.gradle.plugin",
        )
        includeModule("com.gradle", "develocity-gradle-plugin")
        includeModule("com.gradle.develocity", "com.gradle.develocity.gradle.plugin")
        includeModule("com.diffplug.spotless", "com.diffplug.spotless.gradle.plugin")
        includeModule("io.gitlab.arturbosch.detekt", "io.gitlab.arturbosch.detekt.gradle.plugin")
        includeModule("org.gradle.kotlin.kotlin-dsl", "org.gradle.kotlin.kotlin-dsl.gradle.plugin")
        includeModule("org.gradle.kotlin", "gradle-kotlin-dsl-plugins")
      }
    }
  }
  plugins {
    // Release notes at https://docs.gradle.com/enterprise/gradle-plugin/#release_history
    id("com.gradle.develocity") version "4.1.1"
  }
}

plugins { id("com.gradle.develocity") }

val VERSION_NAME: String by extra.properties

develocity {
  buildScan {
    capture { fileFingerprints.set(true) }

    termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
    termsOfUseAgree.set("yes")

    tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")
    tag(VERSION_NAME)

    buildFinished {
      if (failures.isNotEmpty()) {
        for ((i, failure) in failures.withIndex()) {
          value("Failure ${i + 1}", failure.message)
        }
      }
    }

    obfuscation {
      username { "Redacted" }
      hostname { "Redacted" }
      ipAddresses { addresses -> addresses.map { "0.0.0.0" } }
    }
  }
}

rootProject.name = "circuit-root"

// Please keep these in alphabetical order!
include(
  ":backstack",
  ":circuit-codegen",
  ":circuit-codegen-annotations",
  ":circuit-foundation",
  ":circuit-overlay",
  ":circuit-retained",
  ":circuit-runtime",
  ":circuit-runtime-navigation",
  ":circuit-runtime-presenter",
  ":circuit-runtime-screen",
  ":circuit-runtime-ui",
  ":circuit-shared-elements",
  ":circuit-test",
  ":circuitx:android",
  ":circuitx:effects",
  ":circuitx:gesture-navigation",
  ":circuitx:navigation",
  ":circuitx:overlays",
  ":samples:bottom-navigation",
  ":samples:counter",
  ":samples:counter:apps",
  ":samples:counter:mosaic",
  ":samples:interop",
  ":samples:kotlin-inject",
  ":samples:star",
  ":samples:star:apk",
  ":samples:star:benchmark",
  ":samples:star:coil-rule",
  ":samples:tacos",
  ":samples:tutorial",
  ":internal-runtime",
  ":internal-test-utils",
)

// https://docs.gradle.org/5.6/userguide/groovy_plugin.html#sec:groovy_compilation_avoidance
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")

// https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
