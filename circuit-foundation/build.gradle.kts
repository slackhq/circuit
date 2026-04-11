// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.compose)
  id("circuit.base")
  id("circuit.publish")
}

kotlin {
  // region KMP Targets
  android {
    namespace = "com.slack.circuit.foundation"
    compileSdk = 36
    androidResources { enable = true }
    withHostTest { isIncludeAndroidResources = true }
    withDeviceTest {}
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  js(IR) {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser {
      testTask {
        // https://youtrack.jetbrains.com/issue/CMP-4906
        enabled = false
      }
    }
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
    common {
      group("commonJvm") {
        withAndroid()
        withJvm()
      }
      group("browserCommon") {
        withJs()
        withWasmJs()
      }
    }
  }

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
        api(projects.circuitRuntimeNavigation)
        api(projects.circuitRuntimePresenter)
        api(projects.circuitRuntimeUi)
        api(projects.circuitSharedElements)
        implementation(libs.compose.navigationevent)
      }
    }
    androidMain { dependencies { implementation(libs.androidx.activity.compose) } }
    commonTest {
      dependencies {
        implementation(libs.compose.ui.test)
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)

        implementation(projects.internalTestUtils)
      }
    }
    maybeCreate("commonJvmTest").apply {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.junit)
        implementation(libs.truth)
      }
    }
    jvmTest {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.picnic)
      }
    }
    getByName("androidHostTest") {
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
    getByName("androidDeviceTest") {
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

  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
    }
  }
}

tasks
  .withType<KotlinCompile>()
  .matching { it.name.contains("test", ignoreCase = true) }
  .configureEach {
    compilerOptions {
      freeCompilerArgs.addAll(
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-opt-in=com.slack.circuit.runtime.ExperimentalCircuitApi",
      )

      if (name == "compileAndroidHostTest" || name == "compileAndroidDeviceTest") {
        logger.lifecycle("Configuring parcelize for $name")
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
