import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += listOf(
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    )
  }
}

dependencies {
  kapt(libs.dagger.compiler)
  implementation(projects.circuit)
  implementation(libs.androidx.compose.integration.activity)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.appCompat)
  implementation(libs.bundles.androidx.activity)
  implementation(libs.coil)
  implementation(libs.coil.compose)
  implementation(libs.okhttp)
  implementation(libs.okio)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converters.moshi)

  testImplementation(libs.coroutines.test)
  testImplementation(libs.junit)
  testImplementation(libs.molecule.runtime)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)

//  testImplementation(libs.robolectric)
//  testImplementation("androidx.compose.ui:ui-test-junit4:1.0.5")
//  testImplementation("androidx.test:core:1.4.0")
//  testImplementation("androidx.test:core-ktx:1.4.0")
//  testImplementation("androidx.test:monitor:1.4.0")
//  testImplementation("app.cash.molecule:molecule-testing:0.4.0")
}
