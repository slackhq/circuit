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
    minSdk = 28
    targetSdk = 34
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
  @Suppress("DEPRECATION") // https://issuetracker.google.com/issues/379030055
  from(projects.samples.star.benchmark.dependencyProject)
}

dependencies {
  api(projects.samples.star)
  // Necessary for themes.xml parents, which are all still in the standard MDC artifact
  implementation(libs.material)
  implementation(libs.androidx.profileinstaller)
  baselineProfile(projects.samples.star.benchmark)
  debugImplementation(libs.leakcanary.android)
}
