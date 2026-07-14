// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.vanniktech.maven.publish.MavenPublishBaseExtension

// Maven relocation pointer to circuit-codegen, which handles @SubCircuitInject. Ships no code.
plugins {
  `java-library`
  alias(libs.plugins.mavenPublish)
}

configure<MavenPublishBaseExtension> {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()
  pom {
    distributionManagement {
      relocation {
        groupId.set("com.slack.circuit")
        artifactId.set("circuit-codegen")
        message.set(
          "circuitx-subcircuit-codegen has been merged into circuit-codegen. " +
            "@SubCircuitInject is handled by the main Circuit KSP processor."
        )
      }
    }
  }
}

// Gradle module metadata does not honor Maven <relocation>, and consumers prefer it over the POM.
// Disable it so both Gradle and Maven consumers read the POM and follow the relocation.
tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }
