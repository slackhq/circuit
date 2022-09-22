plugins {
  id("com.android.library")
  kotlin("android")
}

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
}

android {
  namespace = "com.slack.circuit.core"
}

dependencies {
  api(libs.bundles.compose)
  api(projects.backstack)
  api(projects.circuitRetained)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.lifecycle.viewModel.compose)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
