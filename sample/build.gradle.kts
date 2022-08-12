plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  alias(libs.plugins.moshiGradlePlugin)
}

android {
  namespace = "com.slack.circuit.sample"
  compileSdk = 33

  defaultConfig {
    minSdk = 24
    targetSdk = 33
    versionCode = 1
    versionName = "1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testApplicationId = "com.slack.circuit.sample.androidTest"
  }

  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    isCoreLibraryDesugaringEnabled = true
  }

  kotlinOptions {
    jvmTarget = "11"
  }

  buildTypes {
    debug {
      matchingFallbacks += listOf("release")
    }
    release {
      isMinifyEnabled = true
//      signingConfig = signingConfigs.debug
//      proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard.pro'
      matchingFallbacks += listOf("release")
    }
  }
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.6")
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
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
