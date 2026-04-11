// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.squareup.anvil.plugin.AnvilExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import wtf.emulator.DeviceModel
import wtf.emulator.EwExtension

apply(plugin = "circuit.spotless")

val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
val jvmTargetVersion = catalog.findVersion("jvmTarget").get().requiredVersion
val jdkVersion = catalog.findVersion("jdk").get().requiredVersion.removeSuffix("-ea").toInt()

val circuitExtension = extensions.create<CircuitProjectExtension>("circuitProject", project)

// Java configuration
pluginManager.withPlugin("java") {
  configure<JavaPluginExtension> {
    toolchain { languageVersion.set(JavaLanguageVersion.of(jdkVersion)) }
  }

  tasks.withType<JavaCompile>().configureEach { options.release.set(jvmTargetVersion.toInt()) }
}

tasks.withType<Test>().configureEach {
  jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
  systemProperty("java.awt.headless", "true")
}

tasks.withType<JavaExec>().configureEach {
  jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
}

// Kotlin configuration
plugins.withType<KotlinBasePlugin> {
  tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
      allWarningsAsErrors.convention(true)
      when (this) {
        is KotlinJvmCompilerOptions -> {
          jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
          freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            // Match JVM assertion behavior:
            // https://publicobject.com/2019/11/18/kotlins-assert-is-not-like-javas-assert/
            "-Xassertions=jvm",
            // Potentially useful for static analysis tools or annotation processors.
            "-Xemit-jvm-type-annotations",
            // Enable new jvm-default behavior
            // https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
            "-jvm-default=no-compatibility",
            "-Xjspecify-annotations=strict",
            // https://youtrack.jetbrains.com/issue/KT-73255
            "-Xannotation-default-target=param-property",
          )
        }
        is KotlinNativeCompilerOptions -> {
          // Disable warnings-as-errors for native due to KLIB resolver duplicate unique_name
          // warnings when both AndroidX and JetBrains Compose artifacts are on the classpath.
          allWarningsAsErrors.convention(false)
        }
      }

      progressiveMode.set(true)
    }
  }

  if (!project.path.startsWith(":samples") && !project.path.startsWith(":internal")) {
    // Can't use KotlinProjectExtension.explicitApi() due to android projects not using that anymore
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
      if (!name.contains("Test")) {
        compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
      }
    }
  }
}

// Teach Gradle that full guava replaces listenablefuture.
dependencies.modules {
  module("com.google.guava:listenablefuture") { replacedBy("com.google.guava:guava") }
}

// Android auto-apply
pluginManager.withPlugin("com.android.library") { apply(plugin = "circuit.android") }

pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
  apply(plugin = "circuit.android")
}

pluginManager.withPlugin("com.android.application") { apply(plugin = "circuit.android") }

pluginManager.withPlugin("com.android.test") { apply(plugin = "circuit.android") }

// Compose plugin auto-apply and configuration
pluginManager.withPlugin("org.jetbrains.compose") {
  apply(plugin = "org.jetbrains.kotlin.plugin.compose")
  configure<ComposeCompilerGradlePluginExtension> { includeSourceInformation = true }
}

// Kotlin BOM enforcement
pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
  dependencies { add("implementation", platform(catalog.findLibrary("kotlin-bom").get())) }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
  configure<KotlinMultiplatformExtension> {
    sourceSets {
      commonMain {
        dependencies {
          // KGP doesn't support catalogs https://youtrack.jetbrains.com/issue/KT-55351
          implementation(
            // https://youtrack.jetbrains.com/issue/KT-58759
            project.dependencies.platform(
              "org.jetbrains.kotlin:kotlin-bom:${catalog.findVersion("kotlin").get().requiredVersion}"
            )
          )
        }
      }
    }
  }

  // Workaround for missing task dependency in WASM
  val executableCompileSyncTasks = tasks.withType(DefaultIncrementalSyncTask::class.java)
  executableCompileSyncTasks.configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
  tasks.withType(KotlinJsTest::class.java).configureEach {
    mustRunAfter(executableCompileSyncTasks)
  }

  // Compose runtime constraint for published KMP projects with compose
  pluginManager.withPlugin("circuit.publish") {
    if (circuitExtension.hasCompose.get()) {
      project.dependencies {
        constraints {
          add("commonMainApi", "org.jetbrains.compose.runtime:runtime:1.9.0") {
            because(
              "AndroidX publishes multiplatform runtime. The JetBrains artifact is now empty."
            )
          }
        }
      }
    }
  }
}

if (project.rootProject != project) {
  pluginManager.withPlugin("wtf.emulator.gradle") {
    val emulatorWtfToken = providers.gradleProperty("emulatorWtfToken")
    configure<EwExtension> {
      device {
        model = DeviceModel.PIXEL_2_ATD
        version = 30
      }
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
