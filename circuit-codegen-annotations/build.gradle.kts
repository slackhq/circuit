// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
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
  // TODO
//  mingwX64()
  js(IR) {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    moduleName = property("POM_ARTIFACT_ID").toString()
    browser()
  }
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        // TODO this is noisy
//        compileOnly(projects.circuitFoundation) { because("Only here for docs linking") }
        compileOnly(libs.kotlinInject.anvil.runtime)
        api(projects.circuitRuntimeScreen)
      }
    }
    val commonJvm =
      maybeCreate("commonJvm").apply {
        dependsOn(commonMain.get())
        dependencies { compileOnly(libs.hilt) }
      }
    androidMain {
      dependsOn(commonJvm)
    }
    jvmMain {
      dependsOn(commonJvm) }
    nativeMain {
      dependencies {
        compileOnly(libs.kotlinInject.anvil.runtime)
        api(libs.kotlinInject.anvil.runtime)
      }
    }
    // We use a common folder instead of a common source set because there is no commonizer
    // which exposes the browser APIs across these two targets.
    jsMain {
      kotlin.srcDir("src/browserMain/kotlin")

      dependencies {
        compileOnly(libs.kotlinInject.anvil.runtime)
        api(libs.kotlinInject.anvil.runtime)
      }
    }
    wasmJsMain {
      kotlin.srcDir("src/browserMain/kotlin")

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

android { namespace = "com.slack.circuit.codegen.annotations" }
