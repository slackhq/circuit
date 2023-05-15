#!/usr/bin/env bash
set -uo pipefail

# If on CI, add indirect swiftshader arg
gpu_arg=""
if [[ "$CI" == "true" ]]; then
  gpu_arg="-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect"
fi

./gradlew cleanManagedDevices --unused-only &&
    ./gradlew generateReleaseBaselineProfile \
    -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=baselineprofile \
    -Dorg.gradle.workers.max=1 \
    -Pandroid.experimental.testOptions.managedDevices.emulator.showKernelLogging=true \
    "${gpu_arg}" \
    --info