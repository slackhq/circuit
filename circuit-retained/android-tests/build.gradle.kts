/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("plugin.parcelize")
}

android {
  namespace = "com.slack.circuit.retained.androidTest"

  defaultConfig {
    minSdk = 28
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}

configurations.configureEach { println(name) }

dependencies {
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.coroutines)
  implementation(libs.coroutines.android)
  implementation(libs.androidx.core)
  androidTestImplementation(configurations.getByName("implementation"))
  androidTestImplementation(libs.coroutines)
  androidTestImplementation(libs.coroutines.android)
  androidTestImplementation(projects.circuitRetained)
  androidTestImplementation(libs.androidx.compose.integration.activity)
  androidTestImplementation(libs.androidx.compose.ui.testing.junit)
  androidTestImplementation(libs.androidx.compose.foundation)
  androidTestImplementation(libs.androidx.compose.ui.ui)
  androidTestImplementation(libs.androidx.compose.material.material)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.truth)
}
