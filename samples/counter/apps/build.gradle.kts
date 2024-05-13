// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.plugin.parcelize)
}

android {
  namespace = "com.slack.circuit.sample.counter.android"
  defaultConfig {
    minSdk = 31 // For the dynamic m3 theme
    targetSdk = 34
  }
}

androidComponents { beforeVariants { variant -> variant.androidTest.enable = false } }

compose.desktop {
  application { mainClass = "com.slack.circuit.sample.counter.desktop.DesktopCounterCircuitKt" }
}

compose.experimental { web.application {} }

kotlin {
  // region KMP Targets
  androidTarget()
  jvm()
  iosX64()
  iosArm64()
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    moduleName = "counterApp"
    browser { commonWebpackConfig { outputFileName = "counterApp.js" } }
    binaries.executable()
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(projects.samples.counter)
        api(libs.kotlinx.immutable)
        api(projects.circuitFoundation)
      }
    }
    val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }
    val jvmMain by getting { dependencies { implementation(compose.desktop.currentOs) } }
    val androidMain by getting {
      dependencies {
        implementation(libs.androidx.appCompat)
        implementation(libs.bundles.compose.ui)
        implementation(libs.androidx.compose.integration.activity)
        implementation(libs.androidx.compose.integration.materialThemeAdapter)
        implementation(libs.androidx.compose.material.icons)
        implementation(libs.androidx.compose.accompanist.systemUi)
      }
    }
    val wasmJsMain by getting {
      dependencies {
        @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
        implementation(compose.components.resources)
        implementation(compose.ui)
        implementation(compose.runtime)
      }
    }
  }
}
