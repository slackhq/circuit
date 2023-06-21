// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import com.diffplug.spotless.LineEnding
import com.dropbox.gradle.plugins.dependencyguard.DependencyGuardPluginExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

buildscript { dependencies { classpath(platform(libs.kotlin.plugins.bom)) } }

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.agp.application) apply false
  alias(libs.plugins.agp.library) apply false
  alias(libs.plugins.agp.test) apply false
  alias(libs.plugins.anvil) apply false
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.dokka) apply false
  //  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.versionsPlugin)
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.moshiGradlePlugin) apply false
  alias(libs.plugins.dependencyGuard) apply false
  alias(libs.plugins.compose) apply false
  alias(libs.plugins.baselineprofile) apply false
}

val ktfmtVersion = libs.versions.ktfmt.get()
val detektVersion = libs.versions.detekt.get()
val twitterDetektPlugin = libs.detektPlugins.twitterCompose

// Flag to disable Compose's kotlin version check because they're often behind
// Or ahead
// Or if they're the same, do nothing
// It's basically just very noisy.
val composeCompilerKotlinVersion = libs.versions.composeCompilerKotlinVersion.get()
val kotlinVersion = libs.versions.kotlin.get()
val suppressComposeKotlinVersion = kotlinVersion != composeCompilerKotlinVersion

allprojects {
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
        "(import|plugins|buildscript|dependencies|pluginManagement|dependencyResolutionManagement)"
      )
    }
    // Apply license formatting separately for kotlin files so we can prevent it from overwriting
    // copied files
    format("license") {
      licenseHeaderFile(rootProject.file("spotless/spotless.kt"), "(package|@file:)")
      target("src/**/*.kt")
      targetExclude("**/circuit/backstack/**/*.kt")
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
}

val knownBomConfigurations =
  setOf(
    "implementation",
    "testImplementation",
    "androidTestImplementation",
    "compileOnly",
    "testCompileOnly",
    "kapt",
    "ksp",
  )

fun Project.configureComposeBom(dependencyHandler: DependencyHandler) {
  dependencyHandler.apply {
    val composeBom = platform(libs.androidx.compose.bom)
    configurations
      .matching { configuration ->
        knownBomConfigurations.any { configuration.name.contains(it, ignoreCase = true) }
      }
      .configureEach { add(name, composeBom) }
  }
}

subprojects {
  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      toolchain {
        languageVersion.set(
          JavaLanguageVersion.of(libs.versions.jdk.get().removeSuffix("-ea").toInt())
        )
      }
    }

    tasks.withType<JavaCompile>().configureEach { options.release.set(11) }

    // This is the default base plugin applied on all projects, so safe to add this hook here
    configureComposeBom(dependencies)
  }

  val hasCompose = !project.hasProperty("circuit.noCompose")
  plugins.withType<KotlinBasePlugin> {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
      compilerOptions {
        allWarningsAsErrors.set(true)
        if (this is KotlinJvmCompilerOptions) {
          jvmTarget.set(JVM_11)
          // Stub gen copies args from the parent compilation
          if (this@configureEach !is KaptGenerateStubsTask) {
            freeCompilerArgs.addAll(
              "-Xjsr305=strict",
              // Match JVM assertion behavior:
              // https://publicobject.com/2019/11/18/kotlins-assert-is-not-like-javas-assert/
              "-Xassertions=jvm",
              // Potentially useful for static analysis tools or annotation processors.
              "-Xemit-jvm-type-annotations",
              // Enable new jvm-default behavior
              // https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
              "-Xjvm-default=all",
              // https://kotlinlang.org/docs/whatsnew1520.html#support-for-jspecify-nullness-annotations
              "-Xtype-enhancement-improvements-strict-mode",
              "-Xjspecify-annotations=strict",
            )
          }
        }

        progressiveMode.set(true)

        if (hasCompose && suppressComposeKotlinVersion) {
          freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=$kotlinVersion"
          )
        }
      }
    }

    if (hasCompose && !pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
      // A standard android project using compose, we need to force the version again here
      // separate from the ComposeExtension configuration elsewhere.
      dependencies {
        add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
        add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
      }
    }

    if (!project.path.startsWith(":samples")) {
      extensions.configure<KotlinProjectExtension> { explicitApi() }
    }

    // region Detekt
    plugins.apply("io.gitlab.arturbosch.detekt")
    configure<DetektExtension> {
      toolVersion = detektVersion
      allRules = true
      config.from(rootProject.file("config/detekt/detekt.yml"))
      buildUponDefaultConfig = true
    }

    tasks.withType<Detekt>().configureEach {
      jvmTarget = "11"
      reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
      }
    }

    dependencies.add("detektPlugins", twitterDetektPlugin)
    // endregion
  }

  pluginManager.withPlugin("com.vanniktech.maven.publish") {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTask>().configureEach {
      outputDirectory.set(rootProject.rootDir.resolve("docs/api/0.x"))
      dokkaSourceSets.configureEach {
        if (name.contains("androidTest", ignoreCase = true)) {
          suppress.set(true)
        }
        skipDeprecated.set(true)
        // AndroidX and Android docs are automatically added by the Dokka plugin.
      }
    }

    apply(plugin = "com.dropbox.dependency-guard")
    configure<DependencyGuardPluginExtension> {
      if (project.name == "circuit-codegen") {
        configuration("runtimeClasspath") {
          baselineMap = {
            // Remove the version
            it.substringBeforeLast(":")
          }
        }
      } else {
        configuration("androidReleaseRuntimeClasspath") {
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

    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
    }
  }

  // Common android config
  val commonAndroidConfig: CommonExtension<*, *, *, *>.() -> Unit = {
    // Don't force compile SDK 34 yet in circuit artifacts yet
    compileSdk =
      if (project.path.startsWith(":samples")) {
        34
      } else {
        33
      }

    if (hasCompose) {
      buildFeatures { compose = true }
      composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }
    }

    compileOptions {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
      // https://issuetracker.google.com/issues/243267012
      disable += "Instantiatable"
    }
  }

  // Android library config
  pluginManager.withPlugin("com.android.library") {
    with(extensions.getByType<LibraryExtension>()) {
      commonAndroidConfig()
      defaultConfig { minSdk = 21 }
    }

    // Single-variant libraries
    extensions.configure<LibraryAndroidComponentsExtension> {
      beforeVariants { builder ->
        if (builder.buildType == "debug") {
          builder.enable = false
        }
      }
    }
  }

  pluginManager.withPlugin("com.android.test") {
    with(extensions.getByType<TestExtension>()) {
      commonAndroidConfig()
      defaultConfig { minSdk = 28 }
    }
  }

  // Android app config
  pluginManager.withPlugin("com.android.application") {
    with(extensions.getByType<ApplicationExtension>()) {
      commonAndroidConfig()
      buildTypes {
        maybeCreate("debug").apply { matchingFallbacks += listOf("release") }
        maybeCreate("release").apply {
          isMinifyEnabled = true
          signingConfig = signingConfigs.getByName("debug")
          matchingFallbacks += listOf("release")
        }
      }
      compileOptions { isCoreLibraryDesugaringEnabled = true }
    }
    dependencies.add("coreLibraryDesugaring", libs.desugarJdkLibs)
  }

  // Disable compose-jb Compose version checks
  pluginManager.withPlugin("org.jetbrains.compose") {
    configure<ComposeExtension> {
      // Sometimes we build against JB's compose compiler and sometimes Google's, whichever has
      // better support for the kotlin version we're on

      // Google version
      //      kotlinCompilerPlugin.set(libs.androidx.compose.compiler.get().toString())

      // JB version
      kotlinCompilerPlugin.set(libs.compose.compilerJb.get().toString())
    }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    // Enforce Kotlin BOM
    dependencies { add("implementation", platform(libs.kotlin.bom)) }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.android") {
    // Enforce Kotlin BOM
    dependencies { add("implementation", platform(libs.kotlin.bom)) }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    // Enforce Kotlin BOM
    configure<KotlinMultiplatformExtension> {
      sourceSets {
        maybeCreate("commonMain").dependencies {
          // KGP doesn't support catalogs https://youtrack.jetbrains.com/issue/KT-55351
          implementation(platform("org.jetbrains.kotlin:kotlin-bom:${libs.versions.kotlin.get()}"))
        }
      }
    }
    tasks.withType<KotlinNativeCompile>().configureEach {
      notCompatibleWithConfigurationCache("https://youtrack.jetbrains.com/issue/KT-49933")
    }
    @Suppress("INVISIBLE_REFERENCE")
    tasks.withType<org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkCopy>().configureEach {
      @Suppress("INVISIBLE_MEMBER")
      notCompatibleWithConfigurationCache("https://youtrack.jetbrains.com/issue/KT-49933")
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
