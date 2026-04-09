// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.dropbox.gradle.plugins.dependencyguard.DependencyGuardPluginExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

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
        providers.gradleProperty("POM_SCM_URL").map { scmUrl ->
          "$scmUrl/tree/main/$relPath/src"
        }
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
  } else {
    configuration("releaseRuntimeClasspath") {
      baselineMap = {
        // Remove the version
        it.substringBeforeLast(":")
      }
    }
    if (project.path != ":circuitx:android") {
      // Android-only project
      configuration("jvmRuntimeClasspath") {
        baselineMap = {
          // Remove the version
          it.substringBeforeLast(":")
        }
      }
    }
  }
}

// Maven publish configuration
configure<MavenPublishBaseExtension> {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()
}
