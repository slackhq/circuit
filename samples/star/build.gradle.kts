// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.LibraryExtension
import com.google.devtools.ksp.gradle.KspAATask
import com.google.devtools.ksp.gradle.KspTaskJvm
import java.util.Locale
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.kotlin.plugin.compose)
  alias(libs.plugins.agp.library) apply false
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.plugin.parcelize) apply false
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.anvil)
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.emulatorWtf)
}

// Cannot enable both Desktop and Android w/ kapt due to
// https://youtrack.jetbrains.com/issue/KT-30878
val buildDesktop = project.hasProperty("circuit.buildDesktop")

// Roborazzi and check annoyingly depend on JVM tasks too
val disableJvmTarget =
  project.gradle.startParameter.taskNames.any {
    it.contains("roborazzi", ignoreCase = true) || it == "check"
  }

if (!buildDesktop) {
  apply(plugin = libs.plugins.agp.library.get().pluginId)
  apply(plugin = libs.plugins.kotlin.plugin.parcelize.get().pluginId)
} else {
  compose { desktop { application { mainClass = "com.slack.circuit.star.MainKt" } } }
}

anvil { kspContributingAnnotations.add("com.slack.circuit.codegen.annotations.CircuitInject") }

kotlin {
  if (buildDesktop) {
    jvm { withJava() }
  } else {
    androidTarget { publishLibraryVariants("release") }
    if (!disableJvmTarget) {
      jvm()
    }
  }
  jvmToolchain(libs.versions.jdk.get().toInt())

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("jvmCommon") {
        withAndroidTarget()
        withJvm()
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.coil)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material.material)
        implementation(libs.compose.material.material3)
        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.compose.uiUtil)
        implementation(libs.coroutines)
        implementation(libs.kotlinx.immutable)
        implementation(libs.ksoup)
        implementation(libs.ktor.client)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.client.auth)
        implementation(libs.ktor.serialization.json)
        implementation(libs.okio)
        implementation(libs.kotlinx.datetime)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.primitiveAdapters)
        implementation(libs.windowSizeClass)
        implementation(compose.components.resources)
        implementation(projects.circuitCodegenAnnotations)
        implementation(projects.circuitFoundation)
        implementation(projects.circuitOverlay)
        implementation(projects.circuitRetained)
        implementation(projects.circuitx.gestureNavigation)
        implementation(projects.circuitx.overlays)
        implementation(libs.eithernet)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(libs.okio.fakefilesystem)
        implementation(libs.testing.assertk)
        implementation(projects.circuitTest)
        implementation(libs.eithernet.testFixtures)
      }
    }
    maybeCreate("jvmCommonMain").apply {
      dependencies {
        api(libs.anvil.annotations)
        api(libs.anvil.annotations.optional)
        implementation(libs.compose.material.icons)
        implementation(libs.dagger)
        implementation(libs.jsoup)
        implementation(libs.coil.network.okhttp)
        implementation(libs.ktor.client.engine.okhttp)
        implementation(libs.okhttp)
        implementation(libs.okhttp.loggingInterceptor)
        val kapt by configurations.getting
        kapt.dependencies.addLater(libs.dagger.compiler)
      }
    }
    maybeCreate("jvmCommonTest").apply {
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
      }
    }
    if (!buildDesktop) {
      androidMain {
        dependencies {
          implementation(libs.androidx.appCompat)
          implementation(libs.androidx.activity.compose)
          implementation(libs.androidx.browser)
          implementation(libs.androidx.compose.accompanist.flowlayout)
          implementation(libs.androidx.compose.accompanist.pager)
          implementation(libs.androidx.compose.accompanist.pager.indicators)
          implementation(libs.androidx.compose.accompanist.systemUi)
          implementation(libs.androidx.compose.googleFonts)
          implementation(libs.androidx.compose.ui.tooling)
          implementation(libs.coroutines.android)
          implementation(libs.sqldelight.driver.android)
          implementation(libs.telephoto.zoomableImageCoil)
          implementation(projects.circuitx.android)
        }
      }
      val androidUnitTest by getting {
        dependencies {
          implementation(libs.androidx.compose.ui.testing.junit)
          implementation(libs.androidx.compose.ui.testing.manifest)
          implementation(libs.androidx.loader)
          implementation(libs.leakcanary.android.instrumentation)
          implementation(libs.robolectric)
          // TODO eventually use KMP support (Android and JVM)
          //  https://github.com/takahirom/roborazzi#experimental-feature-compose-desktop-support
          implementation(libs.roborazzi)
          implementation(libs.roborazzi.compose)
          implementation(libs.roborazzi.rules)
          implementation(libs.androidx.test.espresso.core)
          implementation(projects.samples.star.coilRule)
        }
      }
      val androidInstrumentedTest by getting {
        dependsOn(commonTest.get())
        // Annoyingly cannot depend on commonJvmTest
        dependencies {
          implementation(libs.androidx.compose.ui.testing.junit)
          implementation(libs.androidx.compose.ui.testing.manifest)
          implementation(libs.coroutines.test)
          implementation(libs.junit)
          implementation(libs.leakcanary.android.instrumentation)
          implementation(libs.truth)
          implementation(projects.circuitTest)
          implementation(projects.samples.star.coilRule)
        }
      }
    }
    if (!disableJvmTarget) {
      jvmMain {
        dependencies {
          implementation(compose.desktop.currentOs)
          implementation(libs.appDirs)
          implementation(libs.coroutines.swing)
          implementation(libs.sqldelight.driver.jdbc)
        }
      }
    }

    configureEach {
      @OptIn(ExperimentalKotlinGradlePluginApi::class)
      compilerOptions {
        optIn.addAll(
          "androidx.compose.material.ExperimentalMaterialApi",
          "androidx.compose.material3.ExperimentalMaterial3Api",
          "kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")

        if (this is KotlinJvmCompilerOptions) {
          jvmTarget.set(libs.versions.jvmTarget.map { JvmTarget.fromTarget(it) })
        }
      }
    }
  }

  targets.configureEach {
    if (platformType == KotlinPlatformType.androidJvm) {
      compilations.configureEach {
        compileTaskProvider.configure {
          compilerOptions {
            freeCompilerArgs.addAll(
              "-P",
              "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.slack.circuit.star.parcel.CommonParcelize",
            )
          }
        }
      }
    }
  }
}

if (project.hasProperty("circuit.enableComposeCompilerReports")) {
  val metricsDir = project.layout.buildDirectory.dir("compose_metrics")
  composeCompiler {
    metricsDestination.set(metricsDir)
    reportsDestination.set(metricsDir)
  }
}

if (!buildDesktop) {
  configure<LibraryExtension> {
    namespace = "com.slack.circuit.star"

    defaultConfig {
      minSdk = 28
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      testApplicationId = "com.slack.circuit.star.apk.androidTest"
    }

    testOptions { unitTests.isIncludeAndroidResources = true }
    testBuildType = "release"
  }
} else {
  tasks.withType<JavaCompile>().configureEach {
    options.release.set(libs.versions.jvmTarget.map { it.toInt() })
  }
}

compose {
  desktop { application { mainClass = "com.slack.circuit.star.MainKt" } }
  resources {
    packageOfResClass = "com.slack.circuit.star.resources"
    generateResClass = always
  }
}

sqldelight { databases { create("StarDatabase") { packageName.set("com.slack.circuit.star.db") } } }

// This is the worst deprecation replacement in the history of deprecation replacements
fun String.capitalizeUS() = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}

val kspTargets = kotlin.targets.names.map { it.capitalizeUS() }

// Workaround for https://youtrack.jetbrains.com/issue/KT-59220
afterEvaluate {
  for (target in kspTargets) {
    if (target != "Android" && target != "Jvm") continue
    val buildType = if (target == "Android") "Release" else ""
    val kspTaskName = "ksp${buildType}Kotlin${target}"
    val useKSP2 = providers.gradleProperty("ksp.useKSP2").getOrElse("false").toBoolean()
    val generatedKspKotlinFiles =
      if (useKSP2) {
        val kspReleaseTask = tasks.named<KspAATask>(kspTaskName)
        kspReleaseTask.flatMap { it.kspConfig.kotlinOutputDir }
      } else {
        val kspReleaseTask = tasks.named<KspTaskJvm>(kspTaskName)
        kspReleaseTask.flatMap { it.destination }
      }
    tasks.named<KotlinCompile>("kaptGenerateStubs${buildType}Kotlin${target}").configure {
      source(generatedKspKotlinFiles)
    }
  }
}

ksp { arg("circuit.codegen.lenient", "true") }

dependencies {
  for (target in kspTargets) {
    val targetConfigSuffix = if (target == "Metadata") "CommonMainMetadata" else target
    add("ksp${targetConfigSuffix}", projects.circuitCodegen)
  }
}
