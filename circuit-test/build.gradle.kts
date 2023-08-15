// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.agp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.compose)
  alias(libs.plugins.mavenPublish)
}

kotlin {
  // region KMP Targets
  androidTarget { publishLibraryVariants("release") }
  jvm()
  ios()
  iosSimulatorArm64()
  js(IR) {
    moduleName = property("POM_ARTIFACT_ID").toString()
    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
      }
    }
    browser()
    nodejs()
  }
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuitFoundation)
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(libs.turbine)
        api(libs.molecule.runtime)
      }
    }

    commonTest { dependencies { implementation(libs.coroutines.test) } }

    val jvmTest by getting {
      dependencies {
        implementation(libs.coroutines.test)
        implementation(libs.junit)
        implementation(libs.truth)
        implementation(libs.testing.testParameterInjector)
      }
    }
    with(getByName("androidUnitTest")) { dependsOn(jvmTest) }
  }
}

android { namespace = "com.slack.circuit.test" }
