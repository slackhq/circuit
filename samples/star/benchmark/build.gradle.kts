plugins {
  id("com.android.test")
}

android {
  namespace = "com.circuit.samples.star.benchmark"
  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    // This benchmark buildType is used for benchmarking, and should function like your
    // release build (for example, with minification on). It"s signed with a debug key
    // for easy local/CI testing.
    create("benchmark") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks += listOf("release")
    }
  }

  targetProjectPath = ":samples:star:apk"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
  implementation("androidx.test.ext:junit:1.1.4-rc01")
  implementation("androidx.test.espresso:espresso-core:3.5.0-rc01")
  implementation("androidx.test.uiautomator:uiautomator:2.3.0-alpha01")
  implementation("androidx.benchmark:benchmark-macro-junit4:1.2.0-alpha06")
}

androidComponents {
  beforeVariants {
    it.enable = it.buildType == "benchmark"
  }
}