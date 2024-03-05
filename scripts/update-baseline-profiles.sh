#!/usr/bin/env bash
set -uo pipefail

./gradlew cleanManagedDevices && ./gradlew generateBaselineProfile