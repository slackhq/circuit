// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("com.android.library")
  kotlin("android")
}

android { namespace = "com.slack.circuit.sample.coil.test" }

dependencies {
  api(libs.junit)
  api(libs.coil)
  api(libs.coil.test)
  implementation(libs.androidx.test.monitor)
}
