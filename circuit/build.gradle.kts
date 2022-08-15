plugins {
  id("com.android.library")
  kotlin("android")
  id("com.squareup.anvil")
}

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
}

anvil {
  generateDaggerFactories.set(true)
  generateDaggerFactoriesOnly.set(true)
}

android {
  namespace = "com.slack.circuit.core"
}

dependencies {
  api(libs.androidx.compose.integration.viewModel)
  api(libs.androidx.compose.integration.activity)
  api(libs.dagger)
  api(libs.bundles.compose)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
