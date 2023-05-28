// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
  alias(libs.plugins.agp.test)
  kotlin("android")
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "com.circuit.samples.star.benchmark"
  defaultConfig {
    targetSdk = 33
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // TODO temporary until AGP 8.2, which no longer requires this.
    //  This is because when we update baseline profiles, we do them on emulators but they
    //  run all tests.
    testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
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

  testOptions.managedDevices.devices {
    create<ManagedVirtualDevice>("pixel6Api31") {
      device = "Pixel 6"
      apiLevel = 31
      systemImageSource = "aosp"
    }
  }

  targetProjectPath = ":samples:star:apk"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
  // This specifies the managed devices to use that you run the tests on. The default
  // is none.
  managedDevices += "pixel6Api31"

  // This enables using connected devices to generate profiles. The default is true.
  // When using connected devices, they must be rooted or API 33 and higher.
  useConnectedDevices = false

  // Set to true to see the emulator, useful for debugging. Only enabled locally
  enableEmulatorDisplay = false
}

dependencies {
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.androidx.test.espresso.core)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit)
  implementation(libs.androidx.profileinstaller)
}
