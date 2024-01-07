// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.LibraryExtension
import com.google.devtools.ksp.gradle.KspTaskJvm
import java.util.Locale
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.library) apply false
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.plugin.parcelize) apply false
  alias(libs.plugins.moshiGradlePlugin)
  alias(libs.plugins.anvil)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
}

// Cannot enable both Desktop and Android w/ kapt due to
// https://youtrack.jetbrains.com/issue/KT-30878
val buildDesktop = project.hasProperty("circuit.buildDesktop")

if (!buildDesktop) {
  apply(plugin = libs.plugins.agp.library.get().pluginId)
  apply(plugin = libs.plugins.kotlin.plugin.parcelize.get().pluginId)
}

kotlin {
  if (!buildDesktop) {
    androidTarget { publishLibraryVariants("release") }
    jvm()
  } else {
    jvm { withJava() }
  }
  jvmToolchain(libs.versions.jdk.get().toInt())

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.coil3)
        implementation(libs.coil3.compose)
        implementation(libs.coil3.network)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material.material)
        implementation(libs.compose.material.material3)
        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.compose.uiUtil)
        implementation(libs.coroutines)
        implementation(libs.kotlinx.immutable)
        implementation(libs.ktor.client)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.okio)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.primitiveAdapters)
        implementation(libs.windowSizeClass)
        @OptIn(ExperimentalComposeLibrary::class) implementation(compose.components.resources)
        implementation(projects.circuitCodegenAnnotations)
        implementation(projects.circuitFoundation)
        implementation(projects.circuitOverlay)
        implementation(projects.circuitRetained)
        implementation(projects.circuitx.gestureNavigation)
        implementation(projects.circuitx.overlays)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(projects.circuitTest)
      }
    }
    val commonJvm by creating {
      dependsOn(commonMain.get())
      dependencies {
        api(libs.anvil.annotations)
        api(libs.anvil.annotations.optional)
        implementation(libs.compose.material.icons)
        implementation(libs.compose.material.iconsExtended)
        implementation(libs.dagger)
        implementation(libs.eithernet)
        implementation(libs.jsoup)
        implementation(libs.ktor.client.engine.okhttp)
        implementation(libs.okhttp)
        implementation(libs.okhttp.loggingInterceptor)
        implementation(libs.retrofit)
        implementation(libs.retrofit.converters.moshi)
        val kapt by configurations.getting
        kapt.dependencies.addLater(libs.dagger.compiler)
      }
    }
    val commonJvmTest by creating {
      dependsOn(commonTest.get())
      dependencies {
        implementation(dependencies.testFixtures(libs.eithernet))
        implementation(libs.junit)
        implementation(libs.testing.hamcrest)
        implementation(libs.truth)
      }
    }
    if (!buildDesktop) {
      androidMain {
        dependsOn(commonJvm)
        dependencies {
          implementation(libs.androidx.appCompat)
          implementation(libs.androidx.browser)
          implementation(libs.androidx.compose.accompanist.flowlayout)
          implementation(libs.androidx.compose.accompanist.pager)
          implementation(libs.androidx.compose.accompanist.pager.indicators)
          implementation(libs.androidx.compose.accompanist.systemUi)
          implementation(libs.androidx.compose.integration.activity)
          implementation(libs.androidx.compose.integration.activity)
          implementation(libs.androidx.compose.ui.tooling)
          implementation(libs.coroutines.android)
          implementation(libs.sqldelight.driver.android)
          implementation(libs.telephoto.zoomableImageCoil)
          implementation(projects.circuitx.android)
        }
      }
      val androidUnitTest by getting {
        dependsOn(commonJvmTest)
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
          implementation(libs.testing.espresso.core)
          implementation(projects.samples.star.coilRule)
        }
      }
      val androidInstrumentedTest by getting {
        // Annoyingly cannot depend on commonJvmTest
        dependencies {
          implementation(libs.androidx.compose.ui.testing.junit)
          implementation(libs.androidx.compose.ui.testing.manifest)
          implementation(libs.coroutines.test)
          implementation(libs.junit)
          implementation(libs.leakcanary.android.instrumentation)
          implementation(libs.testing.hamcrest)
          implementation(libs.truth)
          implementation(projects.circuitTest)
          implementation(projects.samples.star.coilRule)
        }
      }
    }
    jvmMain {
      dependsOn(commonJvm)
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.coroutines.swing)
        implementation(libs.sqldelight.driver.jdbc)
      }
    }
    jvmTest { dependsOn(commonJvmTest) }

    configureEach {
      @OptIn(ExperimentalKotlinGradlePluginApi::class)
      compilerOptions {
        optIn.addAll(
          "androidx.compose.material.ExperimentalMaterialApi",
          "androidx.compose.material3.ExperimentalMaterial3Api",
          "coil3.annotation.ExperimentalCoilApi",
          "kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
        freeCompilerArgs.addAll("-Xexpect-actual-classes")

        if (project.hasProperty("circuit.enableComposeCompilerReports")) {
          val metricsDir =
            project.layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath
          freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$metricsDir",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir"
          )
        }

        if (this is KotlinJvmCompilerOptions) {
          jvmTarget.set(libs.versions.jvmTarget.map { JvmTarget.fromTarget(it) })
        }
      }
    }
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

compose.desktop { application { mainClass = "com.slack.circuit.star.MainKt" } }

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
    val kspReleaseTask = tasks.named<KspTaskJvm>("ksp${buildType}Kotlin${target}")
    tasks.named<KotlinCompile>("kaptGenerateStubs${buildType}Kotlin${target}").configure {
      source(kspReleaseTask.flatMap { it.destination })
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
