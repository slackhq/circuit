// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.mavenPublish)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  // Anvil/Dagger does not support iOS targets
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosX64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosX64()
  tvosSimulatorArm64()
  macosX64()
  macosArm64()
  linuxArm64()
  linuxX64()
  // TODO https://github.com/evant/kotlin-inject/pull/440
  //  mingwX64()
  js(IR) {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("commonJs") {
        withJs()
        withWasmJs()
      }
      group("commonJvm") {
        withJvm()
        withAndroidTarget()
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        compileOnly(libs.kotlinInject.anvil.runtime)
        api(projects.circuitRuntimeScreen)
      }
    }
    named("commonJvmMain") { dependencies { compileOnly(libs.hilt) } }
    nativeMain {
      dependencies {
        compileOnly(libs.kotlinInject.anvil.runtime)
        api(libs.kotlinInject.anvil.runtime)
      }
    }
    named("commonJsMain") {
      dependencies {
        compileOnly(libs.kotlinInject.anvil.runtime)
        api(libs.kotlinInject.anvil.runtime)
      }
    }
  }

  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
    }
  }
}

android {
  namespace = "com.slack.circuit.codegen.annotations"
  defaultConfig {
    consumerProguardFiles(
      "src/commonJvmMain/resources/META-INF/proguard/circuit-codegen-annotations.pro"
    )
  }
}
