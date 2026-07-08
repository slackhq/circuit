// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.vanniktech.maven.publish.MavenPublishBaseExtension

// This module no longer ships code. `@SubCircuitInject` is handled by the main `circuit-codegen`
// KSP processor. We keep publishing the coordinate as a Maven relocation pointer so existing
// `ksp("com.slack.circuit:circuitx-subcircuit-codegen:<version>")` declarations resolve to
// `circuit-codegen` instead of breaking. Prefer depending on `circuit-codegen` directly.
plugins {
  `java-library`
  alias(libs.plugins.mavenPublish)
}

configure<MavenPublishBaseExtension> {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()
}

// Gradle module metadata does not honor Maven <relocation>, and consumers prefer it over the POM.
// Disable it so both Gradle and Maven consumers read the POM and follow the relocation.
tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }

// Gradle/vanniktech expose no typed relocation API, so append the <relocation> block to the POM
// directly. The version is intentionally omitted so it always relocates to the same version of
// circuit-codegen being published.
afterEvaluate {
  configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
      pom.withXml {
        val distributionManagement = asNode().appendNode("distributionManagement")
        val relocation = distributionManagement.appendNode("relocation")
        relocation.appendNode("groupId", "com.slack.circuit")
        relocation.appendNode("artifactId", "circuit-codegen")
        relocation.appendNode(
          "message",
          "circuitx-subcircuit-codegen has been merged into circuit-codegen. " +
            "@SubCircuitInject is handled by the main Circuit KSP processor.",
        )
      }
    }
  }
}
