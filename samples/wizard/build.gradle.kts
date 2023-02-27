plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.slack.circuit.wizard"
  compileSdk = 33

  defaultConfig {
    minSdk = 33
    targetSdk = 33
    versionCode = 1
    versionName = "1"
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  kapt(libs.dagger.compiler)
  ksp(projects.circuitCodegen)

  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.compose.integration.materialThemeAdapter)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.bundles.compose.ui)
  implementation(libs.dagger)
  implementation(libs.kotlinx.immutable)
  implementation(projects.circuit)
  implementation(projects.circuitCodegenAnnotations)
}