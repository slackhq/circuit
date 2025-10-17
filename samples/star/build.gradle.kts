// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import app.cash.sqldelight.gradle.SqlDelightTask
import com.google.devtools.ksp.gradle.KspAATask
import java.util.Locale
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.kotlin.plugin.compose)
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.emulatorWtf)
  alias(libs.plugins.metro)
  alias(libs.plugins.compose.hotReload)
}

kotlin {
  jvm()
  androidTarget {
    publishLibraryVariants("release")
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
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
        implementation(libs.compose.ui.util)
        implementation(libs.coroutines)
        implementation(libs.ksoup)
        implementation(libs.ktor.client)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.client.auth)
        implementation(libs.ktor.serialization.json)
        implementation(libs.okio)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.primitiveAdapters)
        implementation(libs.windowSizeClass)
        implementation(compose.components.resources)
        implementation(projects.circuitCodegenAnnotations)
        implementation(projects.circuitFoundation)
        implementation(projects.circuitOverlay)
        implementation(projects.circuitRetained)
        implementation(projects.circuitx.gestureNavigation)
        implementation(projects.circuitx.navigation)
        implementation(projects.circuitx.overlays)
        implementation(projects.internalRuntime)
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
        implementation(libs.compose.material.icons)
        implementation(libs.jsoup)
        implementation(libs.coil.network.okhttp)
        implementation(libs.ktor.client.engine.okhttp)
        implementation(libs.okhttp)
        implementation(libs.okhttp.loggingInterceptor)
      }
    }
    maybeCreate("jvmCommonTest").apply {
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
      }
    }
    androidMain {
      dependencies {
        implementation(libs.androidx.appCompat)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.browser)
        implementation(libs.androidx.compose.googleFonts)
        implementation(libs.compose.ui.tooling)
        implementation(libs.coroutines.android)
        implementation(libs.sqldelight.driver.android)
        implementation(libs.telephoto.zoomableImageCoil)
        implementation(projects.circuitx.android)
      }
    }
    val androidUnitTest by getting {
      dependencies {
        implementation(libs.compose.ui.testing.junit)
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
      // Annoyingly cannot depend on commonJvmTest
      dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.coroutines.android)
        implementation(libs.coroutines.test)
        implementation(libs.junit)
        implementation(libs.leakcanary.android.instrumentation)
        implementation(projects.circuitTest)
        implementation(projects.internalTestUtils)
        implementation(projects.samples.star.coilRule)
      }
    }
    jvmMain {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.appDirs)
        implementation(libs.coroutines.swing)
        implementation(libs.slf4jNop)
        implementation(libs.sqldelight.driver.jdbc)
      }
    }

    configureEach {
      @OptIn(ExperimentalKotlinGradlePluginApi::class)
      compilerOptions {
        optIn.addAll(
          "androidx.compose.material.ExperimentalMaterialApi",
          "androidx.compose.material3.ExperimentalMaterial3Api",
          "coil3.annotation.ExperimentalCoilApi",
          "kotlin.time.ExperimentalTime",
          "kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")
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
              "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.slack.circuit.internal.runtime.Parcelize",
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

android {
  namespace = "com.slack.circuit.star"

  defaultConfig {
    minSdk = 30
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testApplicationId = "com.slack.circuit.star.apk.androidTest"
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      // For https://github.com/takahirom/roborazzi/issues/296
      all { it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware" }
    }
  }
  testBuildType = "release"
}

tasks.withType<JavaCompile>().configureEach {
  // Only configure kotlin/jvm tasks with this
  if (name.startsWith("compileJvm")) {
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

ksp { arg("circuit.codegen.mode", "metro") }

dependencies {
  for (target in kspTargets) {
    val targetConfigSuffix = if (target == "Metadata") "CommonMainMetadata" else target
    add("ksp${targetConfigSuffix}", projects.circuitCodegen)
  }
}

// Wiring of source-generating tasks into KSP for Gradle reasons

val generateAccessorsTasks =
  tasks.named {
    it.startsWith("generateResourceAccessors") ||
      it.startsWith("generateActualResourceCollectors") ||
      it.startsWith("generateExpectResourceCollectors") ||
      it.startsWith("generateComposeResClass")
  }

val sqldelightTasks = tasks.withType<SqlDelightTask>()

tasks.withType<KspAATask>().configureEach {
  mustRunAfter(generateAccessorsTasks)
  mustRunAfter(sqldelightTasks)
}
