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

plugins { kotlin("multiplatform") }

kotlin {
  // region KMP Targets
  jvm()
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuit)
        api(libs.coroutines)
      }
    }
    maybeCreate("commonTest").apply { dependencies { implementation(libs.kotlin.test) } }
  }
}

dependencies { add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler) }
