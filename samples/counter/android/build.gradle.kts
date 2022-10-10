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
  id("com.android.application")
  kotlin("android")
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

dependencies {
  implementation(projects.samples.counter)
  implementation(projects.circuit)
  implementation(libs.bundles.compose)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.compose.accompanist.systemUi)
}
