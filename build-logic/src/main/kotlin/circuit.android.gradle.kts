// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
val jvmTargetVersion = catalog.findVersion("jvmTarget").get().requiredVersion
val compileSdkVersion = catalog.findVersion("android-compileSdk").get().requiredVersion.toInt()
val circuitExtension = extensions.getByType<CircuitProjectExtension>()

// Android Library configuration
pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
  pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    (kotlinExtension as KotlinMultiplatformExtension)
      .targets
      .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
      .configureEach {
        compileSdk = compileSdkVersion
        minSdk = 23
        compilations.withType(KotlinMultiplatformAndroidHostTestCompilation::class.java) {
          targetSdk { release(compileSdkVersion) }
        }
        lint {
          // https://issuetracker.google.com/issues/243267012
          disable += "Instantiatable"
          checkTestSources = true
          checkDependencies = false
          lintConfig = rootProject.file("config/lint/lint.xml")
        }
      }
  }
  dependencies { add("lintChecks", catalog.findLibrary("lints-compose").get()) }
}

fun CommonExtension.configureCommonAndroid() {
  compileSdk = compileSdkVersion
}

pluginManager.withPlugin("com.android.test") {
  extensions.configure<TestExtension> {
    configureCommonAndroid()

    defaultConfig { minSdk = 28 }

    compileOptions {
      sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
      targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
    }
    lint {
      // https://issuetracker.google.com/issues/243267012
      disable += "Instantiatable"
      checkTestSources = true
      checkDependencies = false
      lintConfig = rootProject.file("config/lint/lint.xml")
    }
  }
}

// Android (non-KMP) Library configuration
pluginManager.withPlugin("com.android.library") {
  extensions.configure<LibraryExtension> {
    configureCommonAndroid()

    buildFeatures { compose = circuitExtension.hasCompose.get() }

    compileOptions {
      sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
      targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
    }

    defaultConfig { minSdk = 23 }

    lint {
      // https://issuetracker.google.com/issues/243267012
      disable += "Instantiatable"
      checkTestSources = true
      checkDependencies = false
      lintConfig = rootProject.file("config/lint/lint.xml")
    }
  }

  dependencies {
    add("lintChecks", catalog.findLibrary("lints-compose").get())
  }
}

// Android Application configuration
pluginManager.withPlugin("com.android.application") {
  extensions.configure<ApplicationExtension> {
    configureCommonAndroid()

    buildFeatures { compose = circuitExtension.hasCompose.get() }

    compileOptions {
      sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
      targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
      isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
      minSdk = 23
      targetSdk = compileSdkVersion
    }

    buildTypes {
      maybeCreate("debug").apply { matchingFallbacks += listOf("release") }
      maybeCreate("release").apply {
        isMinifyEnabled = true
        signingConfig = signingConfigs.getByName("debug")
        matchingFallbacks += listOf("release")
      }
    }

    // https://issuetracker.google.com/issues/501744304
    // AGP 9 ignores lint configuration on application modules entirely,
    // causing it to lint transitive dependency test sources.
    // Just disable the tasks until this is fixed.
    afterEvaluate {
      tasks.matching { it.name.contains("lint", ignoreCase = true) }.configureEach {
        enabled = false
      }
    }
    lint {
      // https://issuetracker.google.com/issues/243267012
      disable += "Instantiatable"
      checkTestSources = true
      checkDependencies = false
      lintConfig = rootProject.file("config/lint/lint.xml")
    }
  }

  dependencies {
    add("lintChecks", catalog.findLibrary("lints-compose").get())
    add("coreLibraryDesugaring", catalog.findLibrary("desugarJdkLibs").get())
  }
}
