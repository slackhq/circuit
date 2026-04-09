// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

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

  tasks.withType<Test>().configureEach { systemProperty("java.awt.headless", "true") }
}

// Kotlin configuration
plugins.withType<KotlinBasePlugin> {
  tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (this is KaptGenerateStubsTask) {
      // Don't double apply to stub gen
      return@configureEach
    }
    compilerOptions {
      allWarningsAsErrors.convention(true)
      when (this) {
        is KotlinJvmCompilerOptions -> {
          jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
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
              "-jvm-default=no-compatibility",
              "-Xjspecify-annotations=strict",
              // https://youtrack.jetbrains.com/issue/KT-73255
              "-Xannotation-default-target=param-property",
            )
          }
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
    extensions.configure<KotlinProjectExtension> { explicitApi() }
  }
}

// Teach Gradle that full guava replaces listenablefuture.
dependencies.modules {
  module("com.google.guava:listenablefuture") { replacedBy("com.google.guava:guava") }
}

// Android auto-apply
pluginManager.withPlugin("com.android.library") { apply(plugin = "circuit.android") }

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

pluginManager.withPlugin("org.jetbrains.kotlin.android") {
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
