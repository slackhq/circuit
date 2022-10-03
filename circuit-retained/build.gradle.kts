import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
  id("com.android.library")
  kotlin("multiplatform")
}

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
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
        api(libs.androidx.lifecycle.viewModel.compose)
        api(libs.androidx.lifecycle.viewModel)
        api(libs.bundles.compose)
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
  namespace = "com.slack.circuit.retained"
}

dependencies {
  add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
}
