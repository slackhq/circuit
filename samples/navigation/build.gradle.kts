plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("plugin.parcelize")
}

android {
  namespace = "com.slack.circuit.sample.navigation"

  defaultConfig {
    applicationId = "com.slack.circuit.sample.navigation"
    minSdk = 33
    compileSdk = 33
    targetSdk = 33
  }
}

dependencies {
  implementation(projects.circuit)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.compose.accompanist.systemUi)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.compose.integration.materialThemeAdapter)
  implementation(libs.bundles.compose.ui)
  debugImplementation(libs.androidx.compose.ui.tooling)
}