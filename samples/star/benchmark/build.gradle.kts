// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
  alias(libs.plugins.agp.test)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "com.circuit.samples.star.benchmark"
  defaultConfig {
    targetSdk = 34
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions.managedDevices.devices {
    create<ManagedVirtualDevice>("pixel6Api33") {
      device = "Pixel 6"
      apiLevel = 33
      systemImageSource = "aosp"
    }
  }

  targetProjectPath = ":samples:star:apk"
  val isCi = providers.environmentVariable("CI").isPresent
  // Load the target app in a separate process so that it can be restarted multiple times, which
  // is necessary for startup benchmarking to work correctly.
  // https://source.android.com/docs/core/tests/development/instr-self-e2e
  experimentalProperties["android.experimental.self-instrumenting"] = true
  experimentalProperties["android.experimental.testOptions.managedDevices.setupTimeoutMinutes"] = 20
  experimentalProperties["android.experimental.androidTest.numManagedDeviceShards"] = 1
  experimentalProperties["android.experimental.testOptions.managedDevices.maxConcurrentDevices"] = 1
  experimentalProperties[
    "android.experimental.testOptions.managedDevices.emulator.showKernelLogging"] = true
  // If on CI, add indirect swiftshader arg
  if (isCi) {
    experimentalProperties["android.testoptions.manageddevices.emulator.gpu"] = "swiftshader_indirect"
  }
}

baselineProfile {
  // This specifies the managed devices to use that you run the tests on. The default
  // is none.
  managedDevices += "pixel6Api33"

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
