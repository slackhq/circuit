// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.dropbox.gradle.plugins.dependencyguard.DependencyGuardPluginExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

apply(plugin = "com.vanniktech.maven.publish")

apply(plugin = "org.jetbrains.dokka")

apply(plugin = "com.dropbox.dependency-guard")

// Dokka configuration
configure<DokkaExtension> {
  moduleName.set(project.path.removePrefix(":").replace(":", "/"))
  basePublicationsDirectory.set(layout.buildDirectory.dir("dokkaDir"))
  dokkaSourceSets.configureEach {
    val readMeProvider = project.layout.projectDirectory.file("README.md")
    if (readMeProvider.asFile.exists()) {
      includes.from(readMeProvider)
    }

    if (name.contains("androidTest", ignoreCase = true)) {
      suppress.set(true)
    }
    skipDeprecated.set(true)
    documentedVisibilities.add(VisibilityModifier.Public)

    // Skip internal packages
    perPackageOption {
      // language=RegExp
      matchingRegex.set(".*\\.internal\\..*")
      suppress.set(true)
    }
    // AndroidX and Android docs are automatically added by the Dokka plugin.

    // Add source links
    sourceLink {
      localDirectory.set(layout.projectDirectory.dir("src"))
      val relPath = rootProject.projectDir.toPath().relativize(projectDir.toPath())
      remoteUrl(
        providers.gradleProperty("POM_SCM_URL").map { scmUrl -> "$scmUrl/tree/main/$relPath/src" }
      )
      remoteLineSuffix.set("#L")
    }
  }
}

// Dependency guard configuration
configure<DependencyGuardPluginExtension> {
  if (project.name == "circuit-codegen") {
    configuration("runtimeClasspath") {
      baselineMap = {
        // Remove the version
        it.substringBeforeLast(":")
      }
    }
  } else if (project.path == ":circuitx:android") {
    // Android-only project
    configuration("releaseRuntimeClasspath") {
      baselineMap = {
        // Remove the version
        it.substringBeforeLast(":")
      }
    }
  } else {
    configuration("androidRuntimeClasspath") {
      baselineMap = {
        // Remove the version
        it.substringBeforeLast(":")
      }
    }
    configuration("jvmRuntimeClasspath") {
      baselineMap = {
        // Remove the version
        it.substringBeforeLast(":")
      }
    }
  }
}

// Maven publish configuration
configure<MavenPublishBaseExtension> {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()
}

fun configureBCV(extension: KotlinBaseExtension) {
  @OptIn(ExperimentalAbiValidation::class)
  (extension as ExtensionAware).extensions.findByName("abiValidation")?.apply {
    when (this) {
      is AbiValidationMultiplatformExtension -> {
        enabled.set(true)
        klib {
          enabled.set(true)
        }
      }
      is AbiValidationExtension -> {
        enabled.set(true)
      }
      else -> error("Unrecognized extension: $javaClass")
    }
    filters {
      exclude {
        annotatedWith.addAll(
          "com.slack.circuit.runtime.InternalCircuitApi",
          "com.slack.circuit.runtime.ExperimentalCircuitApi",
          "com.slack.circuit.test.ExperimentalForInheritanceCircuitTestApi",
        )
        byNames.add("**.internal.**")
      }
    }
  }
}

plugins.withId("org.jetbrains.kotlin.jvm") {
  configureBCV(kotlinExtension) }

plugins.withId("org.jetbrains.kotlin.multiplatform") {
  configureBCV(kotlinExtension) }
