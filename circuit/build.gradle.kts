import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
  id("com.android.library")
  kotlin("multiplatform")
  id("com.vanniktech.maven.publish")
}

kotlin {
  //region KMP Targets
  android {
    publishLibraryVariants("release")
  }
  jvm()
  //endregion

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
        api(libs.bundles.compose)
        implementation(libs.androidx.compose.integration.activity)
      }
    }
    maybeCreate("commonTest").apply {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }
    val commonJvmTest = maybeCreate("commonJvmTest").apply {
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
      }
    }
    maybeCreate("jvmTest").apply {
      dependsOn(commonJvmTest)
    }
  }
}

android {
  namespace = "com.slack.circuit.core"
}

androidComponents {
  beforeVariants { variant -> variant.enableAndroidTest = false }
}

dependencies {
  add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
}
