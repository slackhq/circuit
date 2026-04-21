// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import com.diffplug.spotless.LineEnding

val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
val ktfmtVersion = catalog.findVersion("ktfmt").get().requiredVersion

apply(plugin = "com.diffplug.spotless")

val spotlessFormatters: SpotlessExtension.() -> Unit = {
  lineEndings = LineEnding.PLATFORM_NATIVE

  format("misc") {
    target("*.md", ".gitignore")
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlin {
    target("src/**/*.kt")
    ktfmt(ktfmtVersion).googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
    targetExclude("**/spotless.kt")
  }
  kotlinGradle {
    target("*.kts")
    ktfmt(ktfmtVersion).googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile(
      rootProject.file("spotless/spotless.kt"),
      "(import|plugins|buildscript|dependencies|pluginManagement|dependencyResolutionManagement)",
    )
  }
  // Apply license formatting separately for kotlin files so we can prevent it from overwriting
  // copied files
  format("license") {
    licenseHeaderFile(rootProject.file("spotless/spotless.kt"), "(package|@file:)")
    target("src/**/*.kt")
    targetExclude(
      "**/circuit/backstack/**/*.kt",
      "**/HorizontalPagerIndicator.kt",
      "**/FilterList.kt",
      "**/Remove.kt",
      "**/Pets.kt",
      "**/SystemUiController.kt",
      "**/RetainedStateHolderTest.kt",
      "**/RetainedStateRestorationTester.kt",
    )
  }
}

configure<SpotlessExtension> {
  spotlessFormatters()
  if (project.rootProject == project) {
    predeclareDeps()
  }
}

if (project.rootProject == project) {
  configure<SpotlessExtensionPredeclare> { spotlessFormatters() }
}
