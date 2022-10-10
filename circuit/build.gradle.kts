/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
  id("com.android.library")
  kotlin("multiplatform")
  id("com.vanniktech.maven.publish")
}

kotlin {
  // region KMP Targets
  android { publishLibraryVariants("release") }
  jvm()
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(projects.backstack)
        api(projects.circuitRetained)
      }
    }
    maybeCreate("androidMain").apply {
      dependencies {
        api(libs.bundles.compose.core)
        implementation(libs.androidx.compose.integration.activity)
      }
    }
    maybeCreate("commonTest").apply {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(libs.coroutines.test)
      }
    }
    val commonJvmTest =
      maybeCreate("commonJvmTest").apply {
        dependencies {
          implementation(libs.junit)
          implementation(libs.truth)
        }
      }
    maybeCreate("jvmTest").apply { dependsOn(commonJvmTest) }
  }
}

android { namespace = "com.slack.circuit.core" }

androidComponents { beforeVariants { variant -> variant.enableAndroidTest = false } }

dependencies { add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler) }
