// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.baselineprofile)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  macosX64()
  macosArm64()
  js(IR) {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser {
      // Necessary for tests
      testTask {
        useKarma {
          useChromeHeadless()
          useConfigDirectory(project.projectDir.resolve("karma.config.d").resolve("wasm"))
        }
      }
      binaries.executable()
    }
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    group("browserCommon") {
      withJs()
      withWasmJs()
    }
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions.optIn.add("com.slack.circuit.foundation.DelicateCircuitFoundationApi")

  sourceSets {
    commonMain {
      dependencies {
        api(libs.lifecycle.viewModel.compose)
        api(libs.compose.foundation)
        api(libs.compose.runtime)
        api(libs.compose.ui)
        api(libs.coroutines)
        api(projects.backstack)
        api(projects.circuitRetained)
        api(projects.circuitRuntime)
        api(projects.circuitRuntimePresenter)
        api(projects.circuitRuntimeUi)
        api(projects.circuitSharedElements)
        implementation(libs.compose.ui.backhandler)
      }
    }
    androidMain { dependencies { implementation(libs.androidx.activity.compose) } }
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(libs.coroutines.test)

        implementation(projects.internalTestUtils)
      }
    }
    get("browserCommonMain").dependsOn(commonMain.get())
    get("browserCommonTest").dependsOn(commonTest.get())
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependsOn(commonTest.get())
        dependencies {
          implementation(libs.kotlin.test)
          implementation(libs.compose.ui.testing.junit)
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    jvmTest {
      dependsOn(commonJvmTest)
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.picnic)
      }
    }
    androidUnitTest {
      dependsOn(commonJvmTest)
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
    androidInstrumentedTest {
      dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.ui.testing.manifest)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.coroutines.android)
        implementation(libs.junit)
        implementation(projects.internalTestUtils)
      }
    }
  }
}

tasks
  .withType<KotlinCompile>()
  .matching { it.name.contains("test", ignoreCase = true) }
  .configureEach {
    compilerOptions {
      freeCompilerArgs.addAll("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")

      if (
        name == "compileReleaseUnitTestKotlinAndroid" ||
          name == "compileReleaseAndroidTestKotlinAndroid"
      ) {
        freeCompilerArgs.addAll(
          "-P",
          "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.slack.circuit.internal.runtime.Parcelize",
        )
      }
    }
  }

tasks.withType<Test>().configureEach {
  // Without this PausableStateTest will deadlock, timeout and fail. Seems to be related to when
  // it is ran at the same time as other tests. PausableStateTest is pretty innocuous, so seems
  // to be test infra related. Best guess is that ComposeTestRule doesn't like content which calls
  // a composable with a return value. Needs more investigation.
  setForkEvery(1)
}

android {
  namespace = "com.slack.circuit.foundation"
  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}

baselineProfile {
  mergeIntoMain = true
  saveInSrc = true
  from(project(projects.samples.star.benchmark.path))
  filter { include("com.slack.circuit.foundation.**") }
}
