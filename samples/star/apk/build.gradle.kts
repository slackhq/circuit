// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "com.slack.circuit.sample.star.apk"
  defaultConfig {
    minSdk = 30
    targetSdk = 36
    versionCode = 1
    versionName = "1"
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        file("proguard-rules.pro"),
      )
    }
  }
  experimentalProperties["android.experimental.art-profile-r8-rewriting"] = true
  experimentalProperties["android.experimental.r8.dex-startup-optimization"] = true
}

baselineProfile {
  saveInSrc = true
  dexLayoutOptimization = true
  from(project(projects.samples.star.benchmark.path))
}

dependencies {
  api(projects.samples.star)
  implementation(libs.androidx.profileinstaller)
  baselineProfile(projects.samples.star.benchmark)
  debugImplementation(libs.leakcanary.android)
}
