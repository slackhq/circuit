/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    // We have to declare this here in order for kotlin-facets to be generated in iml files
    // https://youtrack.jetbrains.com/issue/KT-36331
    classpath(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    classpath(libs.agp)
  }
}

plugins {
  alias(libs.plugins.anvil) apply false
  alias(libs.plugins.detekt)
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.dokka) apply false
  //  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.versionsPlugin)
  alias(libs.plugins.dependencyAnalysis)
}

configure<DetektExtension> {
  toolVersion = libs.versions.detekt.get()
  allRules = true
}

tasks.withType<Detekt>().configureEach {
  reports {
    html.required.set(true)
    xml.required.set(true)
    txt.required.set(true)
  }
}

val ktfmtVersion = libs.versions.ktfmt.get()

allprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
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
      target("src/**/*.kts")
      ktfmt(ktfmtVersion).googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
      licenseHeaderFile(
        rootProject.file("spotless/spotless.kt"),
        "(import|plugins|buildscript|dependencies|pluginManagement)"
      )
    }
    // Apply license formatting separately for kotlin files so we can prevent it from overwriting
    // copied files
    format("license") {
      licenseHeaderFile(rootProject.file("spotless/spotless.kt"), "package ")
      target("src/**/*.kt")
      targetExclude("**/circuit/backstack/**/*.kt")
    }
  }
}

subprojects {
  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      toolchain {
        languageVersion.set(
          JavaLanguageVersion.of(libs.versions.java.get().removeSuffix("-ea").toInt())
        )
      }
    }

    tasks.withType<JavaCompile>().configureEach { options.release.set(11) }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.withType<KotlinCompile>().configureEach {
      kotlinOptions {
        allWarningsAsErrors = true
        jvmTarget = "11"
        // We use class SAM conversions because lambdas compiled into invokedynamic are not
        // Serializable, which causes accidental headaches with Gradle configuration caching. It's
        // easier for us to just use the previous anonymous classes behavior
        @Suppress("SuspiciousCollectionReassignment")
        freeCompilerArgs +=
          listOf(
            "-progressive",
            "-Xinline-classes",
            "-Xjsr305=strict",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.experimental.ExperimentalTypeInference",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.time.ExperimentalTime",
            // We should be able to remove this in Kotlin 1.7, yet for some reason it still warns
            // about its use
            // https://youtrack.jetbrains.com/issue/KT-52720
            "-opt-in=kotlin.RequiresOptIn",
            // Match JVM assertion behavior:
            // https://publicobject.com/2019/11/18/kotlins-assert-is-not-like-javas-assert/
            "-Xassertions=jvm",
            // Potentially useful for static analysis tools or annotation processors.
            "-Xemit-jvm-type-annotations",
            "-Xproper-ieee754-comparisons",
            // Enable new jvm-default behavior
            // https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
            "-Xjvm-default=all",
            // https://kotlinlang.org/docs/whatsnew1520.html#support-for-jspecify-nullness-annotations
            "-Xtype-enhancement-improvements-strict-mode",
            "-Xjspecify-annotations=strict",
            // Skip compose version check
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
          )
      }
    }

    extensions.configure<KotlinProjectExtension> { explicitApi() }
  }

  tasks.withType<Detekt>().configureEach { jvmTarget = "11" }

  pluginManager.withPlugin("com.vanniktech.maven.publish") {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTask>().configureEach {
      outputDirectory.set(rootDir.resolve("../docs/0.x"))
      dokkaSourceSets.configureEach {
        skipDeprecated.set(true)
        // TODO link compose+android docs
      }
    }

    // Add our maven repository repo
    configure<PublishingExtension> {
      val url = providers.gradleProperty("SlackRepositoryUrl").get()
      repositories {
        maven {
          name = "SlackRepository"
          setUrl(url)
          credentials(PasswordCredentials::class.java)
        }
      }
    }
  }
}

dependencyAnalysis {
  abi {
    exclusions {
      ignoreInternalPackages()
      ignoreGeneratedCode()
    }
  }
}
