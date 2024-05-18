#!/usr/bin/env bash
set -uo pipefail

./gradlew cleanManagedDevices --unused-only
./gradlew generateBaselineProfile