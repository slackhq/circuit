// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import java.nio.file.FileSystems
import kotlin.io.path.deleteIfExists
import org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_GROUP
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME

plugins {
  kotlin("jvm")
  alias(libs.plugins.kotlin.plugin.serialization)
  id("circuit.base")
  id("circuit.publish")
}

dependencies {
  api(projects.circuitSerialization)

  testImplementation(libs.kotlin.test)
}

val r8Configuration: Configuration =
  configurations.create("r8Configuration") {
    isCanBeConsumed = false
    isCanBeResolved = true
  }

dependencies { r8Configuration(libs.r8) }

abstract class BaseR8Task : JavaExec() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val mainJarProp: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val testJarProp: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val testDependencyFilesProp: ConfigurableFileCollection

  fun r8ArgumentProvider(): CommandLineArgumentProvider {
    return CommandLineArgumentProvider {
      buildList {
        addAll(computeArgs())
        testDependencyFilesProp.files
          .filter { it.isFile }
          .forEach { file -> add(file.absolutePath) }
        add(mainJarProp.get().asFile.absolutePath)
        add(testJarProp.get().asFile.absolutePath)
      }
    }
  }

  abstract fun computeArgs(): Iterable<String>

  fun configureR8Inputs(
    mainJar: Provider<RegularFile>,
    testJar: Provider<RegularFile>,
    testDependencyFiles: Provider<FileCollection>,
  ) {
    mainJarProp.set(mainJar)
    testJarProp.set(testJar)
    testDependencyFilesProp.from(testDependencyFiles)
  }
}

abstract class ExtractR8Rules : BaseR8Task() {
  @get:OutputFile abstract val r8Rules: RegularFileProperty

  override fun computeArgs(): Iterable<String> {
    return buildList {
      add("--rules-output")
      add(r8Rules.get().asFile.absolutePath)
      add("--include-origin-comments")
    }
  }
}

abstract class R8Task : BaseR8Task() {
  @get:Input abstract val javaHome: Property<String>

  @get:InputFile @get:PathSensitive(PathSensitivity.NONE) abstract val r8Rules: RegularFileProperty

  @get:OutputFile abstract val mapping: RegularFileProperty

  @get:OutputFile abstract val r8Jar: RegularFileProperty

  override fun computeArgs(): Iterable<String> {
    return buildList {
      add("--classfile")
      // R8 receives the test jar and every runtime jar as program inputs. Their META-INF resources
      // collide in the single output jar, and testR8 only needs executable classes.
      add("--no-data-resources")
      add("--output")
      add(r8Jar.get().asFile.absolutePath)
      add("--pg-conf")
      add(r8Rules.get().asFile.absolutePath)
      add("--pg-map-output")
      add(mapping.get().asFile.absolutePath)
      add("--lib")
      add(javaHome.get())
    }
  }
}

kotlin.target {
  val target = this
  val testCompilation = target.compilations.named(TEST_COMPILATION_NAME)

  val mainJarTask = tasks.named<Jar>(target.artifactsTaskName)
  val mainJar = mainJarTask.flatMap { it.archiveFile }

  val testJar =
    tasks
      .register<Jar>("${target.name}TestJar") {
        from(testCompilation.map { it.output.allOutputs })
        archiveBaseName = base.archivesName.map { it + '-' + target.name }
        archiveClassifier = "tests"
      }
      .flatMap { it.archiveFile }

  val testDependencyFiles = testCompilation.map { it.runtimeDependencyFiles }

  val extractR8Rules =
    tasks.register<ExtractR8Rules>("extractR8Rules") {
      group = BUILD_GROUP
      description = "Extracts R8 rules from jars on the classpath."

      inputs.files(r8Configuration)
      classpath(r8Configuration)
      mainClass.set("com.android.tools.r8.ExtractR8Rules")

      r8Rules.set(layout.buildDirectory.file("shrinker/r8.txt"))
      configureR8Inputs(mainJar, testJar, testDependencyFiles)
      argumentProviders += r8ArgumentProvider()
    }

  val testJarR8 =
    tasks.register<R8Task>("testJarR8") {
      group = BUILD_GROUP
      description = "Assembles an archive containing the test classes run through R8."

      inputs.files(r8Configuration)
      classpath(r8Configuration)
      mainClass.set("com.android.tools.r8.R8")

      javaHome.set(providers.systemProperty("java.home"))
      r8Rules.set(extractR8Rules.flatMap { it.r8Rules })
      r8Jar.set(layout.buildDirectory.file("libs/${base.archivesName.get()}-testsR8.jar"))
      mapping.set(layout.buildDirectory.file("libs/${base.archivesName.get()}-mapping.txt"))
      configureR8Inputs(mainJar, testJar, testDependencyFiles)
      argumentProviders += r8ArgumentProvider()

      doLast {
        // Quick work around for https://issuetracker.google.com/issues/134372167.
        FileSystems.newFileSystem(r8Jar.get().asFile.toPath(), null as ClassLoader?).use { fs ->
          val root = fs.rootDirectories.first()
          listOf("module-info.class", "META-INF/versions/9/module-info.class").forEach { path ->
            root.resolve(path).deleteIfExists()
          }
        }
      }
    }

  val testR8 =
    tasks.register<Test>("testR8") {
      group = VERIFICATION_GROUP
      description = "Runs the reflective serialization smoke test with R8-processed classes."

      dependsOn(testJarR8)
      classpath = project.files(testJarR8.map { it.r8Jar })
      testClassesDirs = project.files(testCompilation.map { it.output.classesDirs })
      filter {
        includeTestsMatching(
          "com.slack.circuit.serialization.reflect.ReflectiveSerializableCircuitSaverR8Test"
        )
      }
    }

  tasks.named("check").configure { dependsOn(testR8) }
}
