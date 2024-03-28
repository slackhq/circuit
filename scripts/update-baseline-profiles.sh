#!/usr/bin/env bash
set -uo pipefail

# TODO use CC when it's fixed https://issuetracker.google.com/issues/328117812
./gradlew cleanManagedDevices && ./gradlew generateBaselineProfile --no-configuration-cache