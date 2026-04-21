// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
  versionCatalogs { maybeCreate("libs").apply { from(files("../gradle/libs.versions.toml")) } }
}

rootProject.name = "build-logic"
