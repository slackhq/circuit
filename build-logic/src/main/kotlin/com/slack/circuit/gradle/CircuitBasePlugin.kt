package com.slack.circuit.gradle

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
import kotlin.apply
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import wtf.emulator.EwExtension

class CircuitBasePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val libs = target.extensions.getByType(LibrariesForLibs::class.java)
    target.configureAnyProject(libs)
    target.configureSubproject(libs)
  }

  private fun Project.configureAnyProject(libs: LibrariesForLibs) {
    val ktfmtVersion = libs.versions.ktfmt.get()
    pluginManager.apply("com.diffplug.spotless")
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
          rootProject.isolated.projectDirectory.file("spotless/spotless.kt"),
          "(import|plugins|buildscript|dependencies|pluginManagement|dependencyResolutionManagement)",
        )
      }
      // Apply license formatting separately for kotlin files so we can prevent it from overwriting
      // copied files
      format("license") {
        licenseHeaderFile(
          rootProject.isolated.projectDirectory.file("spotless/spotless.kt"),
          "(package|@file:)",
        )
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

  private fun Project.configureSubproject(libs: LibrariesForLibs) {
    val detektVersion = libs.versions.detekt.get()
    val twitterDetektPlugin = libs.detektPlugins.twitterCompose
    val jvmTargetVersion = libs.versions.jvmTarget
    val publishedJvmTargetVersion = libs.versions.publishedJvmTarget

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
    }

    val hasCompose = !project.hasProperty("circuit.noCompose")
    plugins.withType(KotlinBasePlugin::class.java).configureEach {
      tasks.withType<KotlinCompilationTask<*>>().configureEach {
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
              // https://youtrack.jetbrains.com/issue/KT-73255
              "-Xannotation-default-target=param-property",
            )
          }

          progressiveMode.set(true)
        }
      }

      if (!project.path.startsWith(":samples") && !project.path.startsWith(":internal")) {
        configure<KotlinProjectExtension> { explicitApi() }
      }

      // region Detekt
      project.pluginManager.apply("io.gitlab.arturbosch.detekt")
      configure<DetektExtension> {
        toolVersion = detektVersion
        allRules = true
        config.from(rootProject.isolated.projectDirectory.file("config/detekt/detekt.yml"))
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
      pluginManager.apply("org.jetbrains.dokka")

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
            @Suppress("NewApi") // lint is confused in the IDE
            val relPath =
              rootProject.isolated.projectDirectory.asFile.toPath().relativize(projectDir.toPath())
            remoteUrl(
              providers.gradleProperty("POM_SCM_URL").map { scmUrl ->
                "$scmUrl/tree/main/$relPath/src"
              }
            )
            remoteLineSuffix.set("#L")
          }
        }
      }

      pluginManager.apply("com.dropbox.dependency-guard")
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
      compileSdk = 36

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
        lintConfig = rootProject.isolated.projectDirectory.file("config/lint/lint.xml").asFile
      }
      dependencies.add("lintChecks", libs.lints.compose)
    }

    // Android library config
    pluginManager.withPlugin("com.android.library") {
      configure<LibraryExtension> {
        commonAndroidConfig()
        defaultConfig { minSdk = 23 }
        testOptions {
          // TODO update once robolectric supports it
          targetSdk = 35
        }
      }

      // Single-variant libraries
      configure<LibraryAndroidComponentsExtension> {
        beforeVariants { builder ->
          if (builder.buildType == "debug") {
            builder.enable = false
          }
        }
      }
    }

    pluginManager.withPlugin("com.android.test") {
      configure<TestExtension> {
        commonAndroidConfig()
        defaultConfig { minSdk = 28 }
      }
    }

    // Android app config
    pluginManager.withPlugin("com.android.application") {
      configure<ApplicationExtension> {
        commonAndroidConfig()
        defaultConfig {
          minSdk = 23
          targetSdk = 36
        }
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
      pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
      configure<ComposeCompilerGradlePluginExtension> { includeSourceInformation.set(true) }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      // Enforce Kotlin BOM
      dependencies.apply { add("implementation", platform(libs.kotlin.bom)) }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
      // Enforce Kotlin BOM
      dependencies.apply { add("implementation", platform(libs.kotlin.bom)) }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
      // Enforce Kotlin BOM
      configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
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
      // lifecycle task that depends on connectedCheck as an alternative. We do this only on
      // projects
      // that apply emulator.wtf though as we don't want to run _all_ connected checks on CI since
      // that would include benchmarks.
      tasks.register("ciConnectedCheck") { dependsOn("connectedCheck") }
    }

    pluginManager.withPlugin("dev.zacsweers.anvil") {
      configure<AnvilExtension> {
        useKsp(contributesAndFactoryGeneration = true, componentMerging = true)
      }
    }
  }

  private inline fun <reified T> Project.configure(action: Action<T>) {
    project.extensions.configure(T::class.java, action)
  }

  private inline fun <reified T : Task> TaskCollection<Task>.withType(): TaskCollection<T> {
    return withType(T::class.java)
  }
}
