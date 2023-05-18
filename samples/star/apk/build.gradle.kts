// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("com.android.application")
  kotlin("android")
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "com.slack.circuit.sample.star.apk"
  defaultConfig {
    minSdk = 28
    targetSdk = 33
    versionCode = 1
    versionName = "1"
  }
  buildTypes {
    val releaseBuildType =
      getByName("release") {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          file("proguard-rules.pro")
        )
      }

    create("benchmark") {
      initWith(releaseBuildType)
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
  }
}

dependencies {
  api(projects.samples.star)
  implementation(libs.androidx.profileinstaller)
  baselineProfile(projects.samples.star.benchmark)
}
