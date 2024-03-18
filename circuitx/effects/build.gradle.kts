// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
  `java-test-fixtures`
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js(IR) {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  if (hasProperty("enableWasm")) {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
      moduleName = property("POM_ARTIFACT_ID").toString()
      browser()
    }
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        implementation(projects.circuitFoundation)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(projects.circuitTest)
      }
    }
    val iosTest by getting { dependencies { dependsOn(commonTest) } }
    val jsTest by getting { dependencies { dependsOn(commonTest) } }
    val jvmTest by getting { dependencies { dependsOn(commonTest) } }
    val androidUnitTest by getting {
      dependsOn(commonTest)
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
  }
  targets.configureEach {
    compilations.configureEach {
      compilerOptions.configure { freeCompilerArgs.add("-Xexpect-actual-classes") }
    }
  }
}

android {
  namespace = "com.slack.circuitx.sideeffects"

  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions { unitTests.isIncludeAndroidResources = true }
  testBuildType = "release"
}
