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