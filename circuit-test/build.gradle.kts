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
        api(projects.circuit)
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(libs.turbine)
        api(libs.molecule.runtime)
      }
    }
    //    maybeCreate("androidMain").apply {
    //      dependencies {
    //        api(libs.bundles.compose.core)
    //      }
    //    }
  }
}

android { namespace = "com.slack.circuit.test" }

dependencies { add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler) }
