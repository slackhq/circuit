// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension

val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
val jvmTargetVersion = catalog.findVersion("jvmTarget").get().requiredVersion
val circuitExtension = extensions.getByType<CircuitProjectExtension>()

fun CommonExtension<*, *, *, *, *, *>.configureCommonAndroid() {
  compileSdk = 36

  buildFeatures { compose = circuitExtension.hasCompose.get() }

  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
    targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
  }

  lint {
    // https://issuetracker.google.com/issues/243267012
    disable += "Instantiatable"
    checkTestSources = true
    lintConfig = rootProject.file("config/lint/lint.xml")
  }
}

// Android Library configuration
pluginManager.withPlugin("com.android.library") {
  with(extensions.getByType<LibraryExtension>()) {
    configureCommonAndroid()
    defaultConfig { minSdk = 23 }
    testOptions { targetSdk = 36 }
  }

  dependencies { add("lintChecks", catalog.findLibrary("lints-compose").get()) }

  // Single-variant libraries
  extensions.configure<LibraryAndroidComponentsExtension> {
    beforeVariants { builder ->
      if (builder.buildType == "debug") {
        builder.enable = false
      }
    }
  }
}

pluginManager.withPlugin("com.android.test") {
  with(extensions.getByType<TestExtension>()) {
    configureCommonAndroid()
    defaultConfig { minSdk = 28 }
  }
}

// Android Application configuration
pluginManager.withPlugin("com.android.application") {
  with(extensions.getByType<ApplicationExtension>()) {
    configureCommonAndroid()
    defaultConfig {
      minSdk = 23
      targetSdk = 36
    }
    buildTypes {
      maybeCreate("debug").apply { matchingFallbacks += listOf("release") }
      maybeCreate("release").apply {
        isMinifyEnabled = true
        signingConfig = signingConfigs.getByName("debug")
        matchingFallbacks += listOf("release")
      }
    }
    compileOptions { isCoreLibraryDesugaringEnabled = true }
  }
  dependencies {
    add("lintChecks", catalog.findLibrary("lints-compose").get())
    add("coreLibraryDesugaring", catalog.findLibrary("desugarJdkLibs").get())
  }
}
