// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
  alias(libs.plugins.agp.test)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.baselineprofile)
  alias(libs.plugins.kotlin.plugin.compose)
}

val mvdApi = 33
val mvdName = "pixel6Api$mvdApi"
val isCi = providers.environmentVariable("CI").isPresent

android {
  namespace = "com.circuit.samples.star.benchmark"
  defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

  testOptions.managedDevices.allDevices {
    create<ManagedVirtualDevice>(mvdName) {
      device = "Pixel 6"
      apiLevel = mvdApi
      systemImageSource = "google"
    }
  }

  targetProjectPath = ":samples:star:apk"
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
    experimentalProperties["android.testoptions.manageddevices.emulator.gpu"] =
      "swiftshader_indirect"
  }
}

val useConnectedDevice =
  providers.gradleProperty("circuit.benchmark.useConnectedDevice").getOrElse("false").toBoolean()

baselineProfile {
  // This specifies the managed devices to use that you run the tests on. The
  // default is none.
  if (!useConnectedDevice) {
    managedDevices += mvdName
  }

  // This enables using connected devices to generate profiles. The default is
  // true. When using connected devices, they must be rooted or API 33 and
  // higher.
  useConnectedDevices = useConnectedDevice

  // Disable the emulator display for GMD devices on CI
  enableEmulatorDisplay = !isCi
}

dependencies {
  implementation(libs.androidx.benchmark.macro.junit)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.test.espresso.core)
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.compose.runtime)
}
