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
import com.squareup.anvil.plugin.AnvilExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import wtf.emulator.EwExtension

buildscript { dependencies { classpath(platform(libs.kotlin.plugins.bom)) } }

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.kotlin.plugin.parcelize) apply false
  alias(libs.plugins.kotlin.plugin.serialization) apply false
  alias(libs.plugins.agp.application) apply false
  alias(libs.plugins.agp.library) apply false
  alias(libs.plugins.agp.test) apply false
  alias(libs.plugins.anvil) apply false
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.dependencyGuard) apply false
  alias(libs.plugins.compose) apply false
  alias(libs.plugins.kotlin.plugin.compose) apply false
  alias(libs.plugins.baselineprofile) apply false
  alias(libs.plugins.emulatorWtf) apply false
  alias(libs.plugins.binaryCompatibilityValidator)
}

val ktfmtVersion = libs.versions.ktfmt.get()
val detektVersion = libs.versions.detekt.get()
val twitterDetektPlugin = libs.detektPlugins.twitterCompose

dokka {
  dokkaPublications.html {
    outputDirectory.set(rootDir.resolve("docs/api/0.x"))
    includes.from(project.layout.projectDirectory.file("README.md"))
  }
}

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

val jvmTargetVersion = libs.versions.jvmTarget
val publishedJvmTargetVersion = libs.versions.publishedJvmTarget

subprojects {
  val isPublished = project.hasProperty("POM_ARTIFACT_ID")
  val jvmTargetProject = if (isPublished) publishedJvmTargetVersion else jvmTargetVersion

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      toolchain {
        languageVersion.set(
          JavaLanguageVersion.of(libs.versions.jdk.get().removeSuffix("-ea").toInt())
        )
      }
    }

    tasks.withType<JavaCompile>().configureEach {
      options.release.set(
        jvmTargetProject.map(JavaVersion::toVersion).map { it.majorVersion.toInt() }
      )
    }

    // This is the default base plugin applied on all projects, so safe to add this hook here
    configureComposeBom(dependencies)
  }

  val hasCompose = !project.hasProperty("circuit.noCompose")
  val useK2Kapt =
    providers.gradleProperty("kapt.use.k2").map { it.toBooleanStrict() }.getOrElse(false)
  plugins.withType<KotlinBasePlugin> {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
      if (this is KaptGenerateStubsTask) {
        if (useK2Kapt) {
          // K2 Kapt is in alpha
          compilerOptions.allWarningsAsErrors.set(false)
        } else {
          compilerOptions {
            progressiveMode.set(false)
            languageVersion.set(KotlinVersion.KOTLIN_1_9)
          }
        }
        // Don't double apply to stub gen
        return@configureEach
      }
      val isWasmTask = name.contains("wasm", ignoreCase = true)
      compilerOptions {
        if (isWasmTask && this is KotlinJsCompilerOptions) {
          // TODO https://youtrack.jetbrains.com/issue/KT-64115
          allWarningsAsErrors.set(false)
        } else if (this is KotlinNativeCompilerOptions) {
          // TODO https://youtrack.jetbrains.com/issue/KT-38719
          allWarningsAsErrors.set(false)
        } else {
          allWarningsAsErrors.set(true)
        }
        if (this is KotlinJvmCompilerOptions) {
          jvmTarget.set(
            jvmTargetProject
              .map(JavaVersion::toVersion)
              .map { it.toString() }
              .map(JvmTarget::fromTarget)
          )
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
      }
    }

    if (!project.path.startsWith(":samples") && !project.path.startsWith(":internal")) {
      extensions.configure<KotlinProjectExtension> { explicitApi() }
    }

    // region Detekt
    project.apply(plugin = "io.gitlab.arturbosch.detekt")
    configure<DetektExtension> {
      toolVersion = detektVersion
      allRules = true
      config.from(rootProject.file("config/detekt/detekt.yml"))
      buildUponDefaultConfig = true
    }

    val buildDir = project.layout.buildDirectory.asFile.get().canonicalPath
    tasks.withType<Detekt>().configureEach {
      jvmTarget = jvmTargetProject.get()
      exclude { it.file.canonicalPath.startsWith(buildDir) }
      reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
      }
    }

    dependencies.add("detektPlugins", twitterDetektPlugin)
    // endregion
  }

  // Teach Gradle that full guava replaces listenablefuture.
  // This bypasses the dependency resolution that transitively bumps listenablefuture to a 9999.0
  // version that is empty.
  dependencies.modules {
    module("com.google.guava:listenablefuture") { replacedBy("com.google.guava:guava") }
  }

  pluginManager.withPlugin("com.vanniktech.maven.publish") {
    apply(plugin = "org.jetbrains.dokka")

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

    apply(plugin = "com.dropbox.dependency-guard")
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
  val commonAndroidConfig: CommonExtension<*, *, *, *, *, *>.() -> Unit = {
    compileSdk = 35

    if (hasCompose) {
      buildFeatures { compose = true }
    }

    compileOptions {
      sourceCompatibility = jvmTargetProject.map(JavaVersion::toVersion).get()
      targetCompatibility = jvmTargetProject.map(JavaVersion::toVersion).get()
    }

    lint {
      // https://issuetracker.google.com/issues/243267012
      disable += "Instantiatable"
      checkTestSources = true
      lintConfig = rootProject.file("config/lint/lint.xml")
    }
    dependencies { add("lintChecks", libs.lints.compose) }
  }

  // Android library config
  pluginManager.withPlugin("com.android.library") {
    with(extensions.getByType<LibraryExtension>()) {
      commonAndroidConfig()
      defaultConfig { minSdk = 21 }
      testOptions { targetSdk = 35 }
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

  pluginManager.withPlugin("org.jetbrains.compose") {
    apply(plugin = "org.jetbrains.kotlin.plugin.compose")
    configure<ComposeCompilerGradlePluginExtension> { includeSourceInformation = true }
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
        commonMain {
          dependencies {
            // KGP doesn't support catalogs https://youtrack.jetbrains.com/issue/KT-55351
            implementation(
              // https://youtrack.jetbrains.com/issue/KT-58759
              project.dependencies.platform(
                "org.jetbrains.kotlin:kotlin-bom:${libs.versions.kotlin.get()}"
              )
            )
          }
        }
      }
    }

    // Workaround for missing task dependency in WASM
    val executableCompileSyncTasks = tasks.withType(DefaultIncrementalSyncTask::class.java)
    tasks.withType(KotlinJsTest::class.java).configureEach {
      mustRunAfter(executableCompileSyncTasks)
    }
  }

  pluginManager.withPlugin("wtf.emulator.gradle") {
    val emulatorWtfToken = providers.gradleProperty("emulatorWtfToken")
    configure<EwExtension> {
      devices.set(listOf(mapOf("model" to "Pixel2Atd", "version" to "30", "atd" to "true")))
      if (emulatorWtfToken.isPresent) {
        token.set(emulatorWtfToken)
      }
    }
    // We don't always run emulator.wtf on CI (forks can't access it), so we add this helper
    // lifecycle task that depends on connectedCheck as an alternative. We do this only on projects
    // that apply emulator.wtf though as we don't want to run _all_ connected checks on CI since
    // that would include benchmarks.
    tasks.register("ciConnectedCheck") { dependsOn("connectedCheck") }
  }

  subprojects {
    pluginManager.withPlugin("dev.zacsweers.anvil") {
      configure<AnvilExtension> {
        useKsp(contributesAndFactoryGeneration = true, componentMerging = true)
      }
    }
  }
}

apiValidation {
  @OptIn(ExperimentalBCVApi::class)
  klib {
    enabled = true
    strictValidation = false
  }
  nonPublicMarkers +=
    setOf(
      "com.slack.circuit.runtime.InternalCircuitApi",
      "com.slack.circuit.runtime.ExperimentalCircuitApi",
      "com.slack.circuit.test.ExperimentalForInheritanceCircuitTestApi",
    )
  ignoredPackages +=
    setOf("com.slack.circuit.foundation.internal", "com.slack.circuit.runtime.internal")
  // Annoyingly this only uses simple names
  // https://github.com/Kotlin/binary-compatibility-validator/issues/16
  ignoredProjects +=
    listOf(
      "apk",
      "apps",
      "benchmark",
      "circuit-codegen",
      "coil-rule",
      "counter",
      "internal-runtime",
      "internal-test-utils",
      "interop",
      "kotlin-inject",
      "mosaic",
      "navigation",
      "star",
      "tacos",
      "tutorial",
    )
}

// Dokka aggregating deps
dependencies {
  dokka(projects.backstack)
  dokka(projects.circuitCodegen)
  dokka(projects.circuitCodegenAnnotations)
  dokka(projects.circuitFoundation)
  dokka(projects.circuitOverlay)
  dokka(projects.circuitRetained)
  dokka(projects.circuitRuntime)
  dokka(projects.circuitRuntimePresenter)
  dokka(projects.circuitRuntimeScreen)
  dokka(projects.circuitRuntimeUi)
  dokka(projects.circuitTest)
  dokka(projects.circuitx.android)
  dokka(projects.circuitx.effects)
  dokka(projects.circuitx.gestureNavigation)
  dokka(projects.circuitx.overlays)
}
