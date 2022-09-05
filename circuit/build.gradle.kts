import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
  id("com.android.library")
  kotlin("multiplatform")
}

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
}

android {
  namespace = "com.slack.circuit.core"
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
      }
    }
    maybeCreate("androidMain").apply {
      dependencies {
        api(projects.backstack)
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
    maybeCreate("androidTest").apply {
      dependsOn(commonJvmTest)
    }
    maybeCreate("jvmTest").apply {
      dependsOn(commonJvmTest)
    }
  }
}

dependencies {
  add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
}
