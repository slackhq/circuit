// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  alias(libs.plugins.emulatorwtf)
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


emulatorwtf {
  // emulator.wtf API token, we recommend either using the EW_API_TOKEN env var
  // instead of this or passing this value in via a project property
  token.set(System.getenv("secrets.EMULATOR_WTF_TOKEN"))

  // where to store results in, they will be further scoped by the variant name,
  // i.e. ./gradlew :app:testFreeDebugWithEmulatorWtf will store outputs in
  // build/build-results/freeDebug
  baseOutputDir.set(layout.buildDirectory.dir("build-results"))

  // Specify what kind of outputs to store in the base output dir
  // default: [OutputType.MERGED_RESULTS_XML, OutputType.COVERAGE, OutputType.PULLED_DIRS]
  val emulatorOutputConfig = listOf(wtf.emulator.OutputType.SUMMARY, wtf.emulator.OutputType.CAPTURED_VIDEO, wtf.emulator.OutputType.LOGCAT)
  outputs.set(emulatorOutputConfig)

  // record a video of the test run
  recordVideo.set(true)

  // ignore test failures and keep running the build
  //
  // NOTE: the build outcome _will_ be success at the end, use the JUnit XML files to
  //       check for test failures
  ignoreFailures.set(true)

  // devices to test on, Defaults to [[model: 'Pixel2', version: 27]]
  val emulatorDeviceConfig = listOf(mapOf(
    "model" to "NexusLowRes",
    "version" to "33"
  ))
  devices.set(emulatorDeviceConfig)

  // whether to enable Android orchestrator, if your app has orchestrator
  // configured this will get picked up automatically, however you can
  // force-change the value here if you want to
  useOrchestrator.set(true)

  // whether to clear package data before running each test (orchestrator only)
  // if your app has this configured via testInstrumentationRunnerArguments then
  // it will get picked up automatically
  clearPackageData.set(true)

  // if true, the Gradle plugin will fetch coverage data and store under
  // `baseOutputDir/${variant}`, if your app has coverage enabled this will be
  // enabled automatically
  withCoverage.set(false)

  // Set to a number larger than 1 to randomly split your tests into multiple
  // shards to be executed in parallel
  numUniformShards.set(2)

  // Set to a number larger than 1 to split your tests into multiple shards
  // based on test counts to be executed in parallel
  numShards.set(2)

  // Set to a non-zero value to repeat device/shards that failed, the repeat
  // attempts will be executed in parallel
  numFlakyTestAttempts.set(1)

  // Enable-disable the test input file cache (APKs etc)
  fileCacheEnabled.set(false)

  // Set the maximum time-to-live of items in the test input file cache
  //fileCacheTtl = Duration.ofHours(3)

  // Disable caching test results in the backend
  // NOTE! This will not disable caching at the Gradle task or Gradle build cache level,
  // use sideEffects = true to disable all caching
  testCacheEnabled.set(false)
}

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
        freeCompilerArgs.addAll(
          "-P",
          "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
            project.buildDir.absolutePath +
            "/compose_metrics"
        )
        freeCompilerArgs.addAll(
          "-P",
          "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
            project.buildDir.absolutePath +
            "/compose_metrics"
        )
      }
    }
  }

dependencies {
  kapt(libs.dagger.compiler)
  ksp(projects.circuitCodegen)
  implementation(projects.circuitCodegenAnnotations)
  implementation(projects.circuitFoundation)
  implementation(projects.circuitOverlay)
  implementation(projects.circuitRetained)
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
  implementation(libs.bundles.androidx.activity)
  implementation(libs.coil)
  implementation(libs.coil.compose)
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
  testImplementation(libs.roborazzi.rules)
  testImplementation(projects.samples.star.coilRule)

  androidTestImplementation(libs.androidx.compose.ui.testing.manifest)
  androidTestImplementation(libs.leakcanary.android.instrumentation)
  androidTestImplementation(libs.androidx.compose.ui.testing.junit)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.coroutines.test)
  androidTestImplementation(libs.truth)
  androidTestImplementation(projects.samples.star.coilRule)
}
