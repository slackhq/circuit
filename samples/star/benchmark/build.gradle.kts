// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("com.android.test")
  kotlin("android")
}

android {
  namespace = "com.circuit.samples.star.benchmark"
  defaultConfig {
    targetSdk = 33
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    // This benchmark buildType is used for benchmarking, and should function like your
    // release build (for example, with minification on). It"s signed with a debug key
    // for easy local/CI testing.
    create("benchmark") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks += listOf("release")
    }
  }

  targetProjectPath = ":samples:star:apk"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.androidx.test.espresso.core)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit)
  implementation(libs.androidx.profileinstaller)
}

androidComponents { beforeVariants { it.enable = it.buildType == "benchmark" } }
