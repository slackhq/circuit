// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.compose.desktop.preview.tasks.AbstractConfigureDesktopPreviewTask

plugins {
  kotlin("multiplatform")
  alias(libs.plugins.compose)
  id("com.android.application")
  kotlin("plugin.parcelize")
}

android {
  namespace = "com.slack.circuit.sample.counter.android"
  defaultConfig {
    compileSdk = 33
    minSdk = 33
    targetSdk = 33
  }
}

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }

compose.desktop {
  application { mainClass = "com.slack.circuit.sample.counter.desktop.DesktopCounterCircuitKt" }
}

tasks.withType<AbstractConfigureDesktopPreviewTask>().configureEach {
  notCompatibleWithConfigurationCache("https://github.com/JetBrains/compose-jb/issues/2376")
}

kotlin {
  // region KMP Targets
  android()
  jvm()
  ios()
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(projects.samples.counter)
        api(libs.kotlinx.immutable)
        api(projects.circuitFoundation)
      }
    }
    maybeCreate("commonTest").apply { dependencies { implementation(libs.kotlin.test) } }
    maybeCreate("jvmMain").apply { dependencies { implementation(compose.desktop.currentOs) } }
    maybeCreate("androidMain").apply {
      dependencies {
        implementation(projects.samples.counter)
        implementation(projects.circuitFoundation)
        implementation(libs.androidx.appCompat)
        implementation(libs.bundles.compose.ui)
        implementation(libs.androidx.compose.integration.activity)
        implementation(libs.androidx.compose.integration.materialThemeAdapter)
        implementation(libs.androidx.compose.material.icons)
        implementation(libs.androidx.compose.material.iconsExtended)
        implementation(libs.androidx.compose.accompanist.systemUi)
      }
    }
  }
}

// dependencies {
//  implementation(compose.desktop.currentOs)
//  implementation(libs.kotlinx.immutable)
//  implementation(projects.samples.counter)
//  implementation(projects.circuitFoundation)
// }
