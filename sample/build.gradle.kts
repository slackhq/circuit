plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  alias(libs.plugins.moshiGradlePlugin)
}

android {
  namespace = "com.slack.circuit.sample"

  defaultConfig {
    minSdk = 24
    targetSdk = 33
    versionCode = 1
    versionName = "1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testApplicationId = "com.slack.circuit.sample.androidTest"
  }
}

dependencies {
  kapt(libs.dagger.compiler)
  implementation(projects.circuit)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.appCompat)
  implementation(libs.bundles.androidx.activity)
  implementation(libs.coil)
  implementation(libs.coil.compose)
  implementation(libs.okhttp)
  implementation(libs.okio)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converters.moshi)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
