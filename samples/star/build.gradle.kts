// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.google.devtools.ksp.gradle.KspTaskJvm
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.moshiGradlePlugin)
  alias(libs.plugins.anvil)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
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

tasks
  .withType<KotlinCompile>()
  .matching { it !is KaptGenerateStubsTask }
  .configureEach {
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
          "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir"
        )
      }
    }
  }

// Workaround for https://youtrack.jetbrains.com/issue/KT-59220
afterEvaluate {
  val kspReleaseTask = tasks.named<KspTaskJvm>("kspReleaseKotlin")
  tasks.named<KotlinCompile>("kaptGenerateStubsReleaseKotlin").configure {
    source(kspReleaseTask.flatMap { it.destination })
  }
}

dependencies {
  kapt(libs.dagger.compiler)
  ksp(projects.circuitCodegen)
  implementation(projects.circuitCodegenAnnotations)
  implementation(projects.circuitFoundation)
  implementation(projects.circuitOverlay)
  implementation(projects.circuitRetained)
  implementation(projects.circuitx.android)
  implementation(projects.circuitx.gestureNavigation)
  implementation(projects.circuitx.overlays)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.browser)
  implementation(libs.bundles.compose.ui)
  implementation(libs.androidx.compose.ui.util)
  implementation(libs.androidx.compose.integration.materialThemeAdapter)
  implementation(libs.androidx.compose.material.icons)
  implementation(libs.androidx.compose.material.iconsExtended)
  implementation(libs.androidx.compose.material.material3.windowSizeClass)
  implementation(libs.androidx.compose.accompanist.pager)
  implementation(libs.androidx.compose.accompanist.pager.indicators)
  implementation(libs.androidx.compose.accompanist.flowlayout)
  implementation(libs.androidx.compose.accompanist.swiperefresh)
  implementation(libs.androidx.compose.accompanist.systemUi)
  implementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.coil)
  implementation(libs.coil.compose)
  implementation(libs.eithernet)
  implementation(libs.okhttp)
  implementation(libs.okhttp.loggingInterceptor)
  implementation(libs.okio)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converters.moshi)
  debugImplementation(libs.leakcanary.android)
  implementation(libs.dagger)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.immutable)
  implementation(libs.jsoup)
  implementation(libs.sqldelight.driver.android)
  implementation(libs.sqldelight.coroutines)
  implementation(libs.sqldelight.primitiveAdapters)
  implementation(libs.telephoto.zoomableImageCoil)

  testImplementation(libs.androidx.compose.ui.testing.junit)
  testImplementation(libs.junit)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.molecule.runtime)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(libs.testing.hamcrest)
  testImplementation(libs.androidx.loader)
  testImplementation(projects.circuitTest)
  testImplementation(libs.testing.espresso.core)
  testImplementation(libs.androidx.compose.ui.testing.manifest)
  testImplementation(libs.leakcanary.android.instrumentation)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.rules)
  testImplementation(testFixtures(libs.eithernet))
  testImplementation(projects.samples.star.coilRule)

  androidTestImplementation(libs.androidx.compose.ui.testing.manifest)
  androidTestImplementation(libs.leakcanary.android.instrumentation)
  androidTestImplementation(libs.androidx.compose.ui.testing.junit)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.coroutines.test)
  androidTestImplementation(libs.truth)
  androidTestImplementation(projects.circuitTest)
  androidTestImplementation(projects.samples.star.coilRule)
}
