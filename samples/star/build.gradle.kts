// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.google.devtools.ksp.gradle.KspTaskJvm
import java.util.Locale
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.moshiGradlePlugin)
  alias(libs.plugins.anvil)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
  // TODO why is it not enough to just have the runtime dep?
  alias(libs.plugins.molecule)
}

kotlin {
  androidTarget { publishLibraryVariants("release") }
  jvm()

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.coil3)
        implementation(libs.coil3.compose)
        implementation(libs.okio)
        implementation(projects.circuitCodegenAnnotations)
        implementation(projects.circuitFoundation)
        implementation(projects.circuitOverlay)
        implementation(projects.circuitRetained)
        implementation(projects.circuitx.gestureNavigation)
        implementation(projects.circuitx.overlays)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material.material)
        implementation(libs.compose.material.material3)
        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
        implementation(libs.compose.uiUtil)
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.kotlinx.immutable)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.primitiveAdapters)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.coroutines.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(projects.circuitTest)
      }
    }
    val commonJvm by creating {
      dependsOn(commonMain.get())
      dependencies {
        implementation(libs.eithernet)
        implementation(libs.okhttp)
        implementation(libs.okhttp.loggingInterceptor)
        implementation(libs.retrofit)
        implementation(libs.retrofit.converters.moshi)
        implementation(libs.dagger)
        implementation(libs.jsoup)
        implementation(libs.androidx.compose.material.material3.windowSizeClass)
        implementation(libs.compose.material.icons)
        implementation(libs.compose.material.iconsExtended)
        val kapt by configurations.getting
        kapt.dependencies.addLater(libs.dagger.compiler)
      }
    }
    val commonJvmTest by creating {
      dependsOn(commonTest.get())
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
        implementation(libs.testing.hamcrest)
        implementation(dependencies.testFixtures(libs.eithernet))
      }
    }
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
        implementation(libs.coil)
        implementation(libs.coil.compose)
        implementation(libs.sqldelight.driver.android)
        implementation(libs.telephoto.zoomableImageCoil)
        implementation(projects.circuitx.android)
      }
    }
    val androidUnitTest by getting {
      dependsOn(commonJvmTest)
      dependencies {
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.robolectric)
        implementation(libs.androidx.loader)
        implementation(libs.testing.espresso.core)
        implementation(libs.androidx.compose.ui.testing.manifest)
        implementation(libs.leakcanary.android.instrumentation)
        implementation(projects.samples.star.coilRule)
        // TODO eventually use KMP support (Android and JVM)
        //  https://github.com/takahirom/roborazzi#experimental-feature-compose-desktop-support
        implementation(libs.roborazzi)
        implementation(libs.roborazzi.compose)
        implementation(libs.roborazzi.rules)
      }
    }
    val androidInstrumentedTest by getting {
      dependsOn(commonJvmTest)
      dependencies {
        implementation(libs.androidx.compose.ui.testing.manifest)
        implementation(libs.leakcanary.android.instrumentation)
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.coroutines.test)
        implementation(projects.circuitTest)
        implementation(projects.samples.star.coilRule)
      }
    }
    jvmMain { dependsOn(commonJvm) }
    jvmTest { dependsOn(commonJvmTest) }

    configureEach {
      @OptIn(ExperimentalKotlinGradlePluginApi::class)
      compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )

        if (project.hasProperty("circuit.enableComposeCompilerReports")) {
          val metricsDir =
              project.layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath
          freeCompilerArgs.addAll(
              "-P",
              "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$metricsDir",
              "-P",
              "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir")
        }
      }
    }
  }
}

android {
  namespace = "com.slack.circuit.star"

  defaultConfig {
    minSdk = 28
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testApplicationId = "com.slack.circuit.star.apk.androidTest"
  }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
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
    val kspReleaseTask = tasks.named<KspTaskJvm>("ksp${buildType}Kotlin${target}")
    tasks.named<KotlinCompile>("kaptGenerateStubs${buildType}Kotlin${target}").configure {
      source(kspReleaseTask.flatMap { it.destination })
    }
  }
}

dependencies {
  for (target in kspTargets) {
    val targetConfigSuffix = if (target == "Metadata") "CommonMainMetadata" else target
    add("ksp${targetConfigSuffix}", projects.circuitCodegen)
  }
}
