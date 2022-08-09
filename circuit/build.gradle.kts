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
  compileSdk = 33

  defaultConfig { minSdk = 21 }

  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
  }

  // TODO single-variant
}

dependencies {
  api(libs.androidx.compose.integration.viewModel)
  api(libs.androidx.compose.integration.activity)
  api(libs.dagger)
  api(libs.bundles.compose)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
