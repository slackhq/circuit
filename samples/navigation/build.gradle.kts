import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.agp.application)
  alias(libs.plugins.kotlin.plugin.parcelize)
}

android {
  namespace = "com.slack.circuit.sample.navigation"
  defaultConfig {
    minSdk = 21
    targetSdk = 35
  }
}

androidComponents { beforeVariants { variant -> variant.androidTest.enable = false } }

kotlin {
  androidTarget()
  jvm {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    mainRun { mainClass.set("com.slack.circuit.sample.navigation.MainKt") }
  }
  jvmToolchain(libs.versions.jdk.get().toInt())

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.compose.foundation)
        implementation(libs.compose.material.material3)
        implementation(libs.compose.material.icons)
        implementation(libs.compose.ui.tooling.preview)
        implementation(projects.circuitFoundation)
      }
    }
    androidMain {
      dependencies {
        implementation(libs.androidx.activity.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.appCompat)
        implementation(libs.bundles.compose.ui)
        implementation(libs.material)
      }
    }
    jvmMain { dependencies { implementation(compose.desktop.currentOs) } }

    configureEach {
      @OptIn(ExperimentalKotlinGradlePluginApi::class)
      compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        if (this is KotlinJvmCompilerOptions) {
          jvmTarget.set(libs.versions.jvmTarget.map { JvmTarget.fromTarget(it) })
        }
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
              "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.slack.circuit.sample.navigation.parcel.CommonParcelize",
            )
          }
        }
      }
    }
  }
}

tasks
  .withType<JavaCompile>()
  .named {
    // Don't set the release flag on the android JavaCompile task
    !it.endsWith("WithJavac")
  }
  .configureEach { options.release.set(libs.versions.jvmTarget.map(String::toInt)) }

compose.desktop { application { mainClass = "com.slack.circuit.sample.navigation.MainKt" } }
