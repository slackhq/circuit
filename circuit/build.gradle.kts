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
  api(libs.androidx.compose.integration.viewModel)
  api(libs.androidx.compose.integration.activity)
  api(libs.bundles.compose)
  api(projects.backstack)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
