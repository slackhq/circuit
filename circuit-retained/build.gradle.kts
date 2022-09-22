plugins {
  id("com.android.library")
  kotlin("android")
}

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
}

android {
  namespace = "com.slack.circuit.retained"
}

dependencies {
  api(libs.androidx.lifecycle.viewModel)
  api(libs.bundles.compose)
}
