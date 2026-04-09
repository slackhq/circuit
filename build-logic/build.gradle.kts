// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  `kotlin-dsl`
}

kotlin { jvmToolchain(libs.versions.jdk.get().removeSuffix("-ea").toInt()) }

dependencies {
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.kotlin.compose.compiler.gradlePlugin)
  compileOnly(libs.agp)
  implementation(
    libs.plugins.mavenPublish.get().run { "$pluginId:$pluginId.gradle.plugin:$version" }
  )
  implementation(libs.plugins.dokka.get().run { "$pluginId:$pluginId.gradle.plugin:$version" })
  implementation(
    libs.plugins.dependencyGuard.get().run { "$pluginId:$pluginId.gradle.plugin:$version" }
  )
  implementation(
    libs.plugins.spotless.get().run { "$pluginId:$pluginId.gradle.plugin:$version" }
  )
}
