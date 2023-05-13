#!/usr/bin/env bash
set -uo pipefail

./gradlew generateReleaseBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=baselineprofile --quiet