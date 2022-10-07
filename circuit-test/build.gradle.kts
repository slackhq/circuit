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
        api(projects.circuit)
        api(libs.compose.runtime)
        api(libs.coroutines)
        api(libs.turbine)
        api(libs.molecule.runtime)
      }
    }
//    maybeCreate("androidMain").apply {
//      dependencies {
//        api(libs.bundles.compose)
//      }
//    }
  }
}

android {
  namespace = "com.slack.circuit.test"
}

dependencies {
  add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
}
