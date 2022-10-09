import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
  kotlin("multiplatform")
}

kotlin {
  //region KMP Targets
  jvm()
  //endregion

  sourceSets {
    commonMain {
      dependencies {
        api(projects.circuit)
        api(libs.coroutines)
      }
    }
    maybeCreate("commonTest").apply {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }
  }
}

dependencies {
  add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
}
