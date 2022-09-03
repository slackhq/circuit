import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("plugin.parcelize")
}

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
}

android {
  namespace = "com.slack.circuit.core"

  testOptions { unitTests.isIncludeAndroidResources = true }
}

tasks
  .withType<KotlinCompile>()
  .matching { it.name.contains("test", ignoreCase = true) }
  .configureEach {
    kotlinOptions {
      @Suppress("SuspiciousCollectionReassignment")
      freeCompilerArgs += listOf("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
  }

dependencies {
  api(libs.androidx.compose.integration.viewModel)
  api(libs.androidx.compose.integration.activity)
  api(libs.bundles.compose)
  api(projects.backstack)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.molecule.runtime)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
}
