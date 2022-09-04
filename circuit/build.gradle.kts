plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("plugin.parcelize")
  alias(libs.plugins.paparazzi)
}

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
}

android {
  namespace = "com.slack.circuit.core"
}

tasks.withType<Test>().configureEach {
  jvmArgs(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
  )
}

dependencies {
  api(libs.androidx.compose.integration.viewModel)
  api(libs.androidx.compose.integration.activity)
  api(libs.bundles.compose)
  api(projects.backstack)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.androidx.compose.ui.ui)
  testImplementation(libs.androidx.compose.material.material3)
}
