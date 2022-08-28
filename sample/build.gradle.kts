import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  alias(libs.plugins.moshiGradlePlugin)
  alias(libs.plugins.anvil)
}

android {
  namespace = "com.slack.circuit.sample"

  defaultConfig {
    minSdk = 28
    targetSdk = 32
    versionCode = 1
    versionName = "1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testApplicationId = "com.slack.circuit.sample.androidTest"
  }

  testOptions { unitTests.isIncludeAndroidResources = true }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs +=
      listOf(
        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
      )
  }
}

dependencies {
  kapt(libs.dagger.compiler)
  implementation(projects.circuit)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.compose.ui.util)
  implementation(libs.androidx.compose.accompanist.pager)
  implementation(libs.androidx.compose.accompanist.pager.indicators)
  implementation(libs.androidx.palette)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.bundles.androidx.activity)
  implementation(libs.coil)
  implementation(libs.coil.compose)
  implementation(libs.okhttp)
  implementation(libs.okhttp.loggingInterceptor)
  implementation(libs.okio)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converters.moshi)

  implementation(libs.dagger)

  testImplementation(libs.androidx.compose.ui.testing.junit)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.junit)
  testImplementation(libs.molecule.runtime)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
}
