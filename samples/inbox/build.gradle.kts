// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import java.util.Locale
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
  alias(libs.plugins.agp.kmp)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.kotlin.plugin.parcelize)
  alias(libs.plugins.ksp)
  alias(libs.plugins.metro)
  id("circuit.base")
}

kotlin {
  android {
    namespace = "com.slack.circuit.sample.inbox"
    compileSdk = 36
    withHostTest {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
  jvm {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    mainRun { mainClass.set("com.slack.circuit.sample.inbox.MainKt") }
  }
  jvmToolchain(libs.versions.jdk.get().toInt())

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.compose.foundation)
        implementation(libs.compose.material.material3)
        implementation(libs.compose.material.icons)
        implementation(libs.compose.navigationevent)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.coroutines)
        implementation(libs.windowSizeClass)
        implementation(projects.circuitCodegenAnnotations)
        implementation(projects.circuitFoundation)
        implementation(projects.circuitRetained)
        implementation(projects.circuitx.effects)
        implementation(projects.internalRuntime)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.molecule.runtime)
        implementation(libs.turbine)
        implementation(libs.testing.assertk)
        implementation(projects.circuitTest)
      }
    }
    androidMain {
      dependencies {
        implementation(libs.androidx.appCompat)
      }
    }
    getByName("androidHostTest") {
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.androidx.compose.ui.testing.manifest)
      }
    }
    jvmMain { dependencies { implementation(compose.desktop.currentOs) } }

    configureEach {
      compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add(
          "androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi"
        )
      }
    }
  }

  targets.configureEach {
    if (platformType == KotlinPlatformType.androidJvm) {
      compilations.configureEach {
        compileTaskProvider.configure {
          compilerOptions {
            freeCompilerArgs.addAll(
              "-P",
              "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.slack.circuit.internal.runtime.Parcelize",
            )
          }
        }
      }
    }
  }
}

compose.desktop { application { mainClass = "com.slack.circuit.sample.inbox.MainKt" } }

metro { @OptIn(ExperimentalMetroGradleApi::class) enableFunctionProviders.set(true) }

ksp { arg("circuit.codegen.mode", "metro") }

private fun String.capitalizeUS() = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}

dependencies {
  for (target in kotlin.targets.names.map { it.capitalizeUS() }) {
    val targetConfigSuffix = if (target == "Metadata") "CommonMainMetadata" else target
    add("ksp${targetConfigSuffix}", projects.circuitCodegen)
  }
}
