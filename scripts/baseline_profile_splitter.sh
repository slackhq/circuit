#!/usr/bin/env bash

# This script splits the baseline profile into separate files for each circuit artifact
# and then removes moved lines from the original core artifact.
#
# Usage: ./scripts/baseline_profile_splitter.sh

removeLines() (
  remove_lines="$1"
  all_lines="$2"
  tmp_file="$(mktemp)"
  grep -Fvxf "$remove_lines" "$all_lines" >"$tmp_file"
  mv "$tmp_file" "$all_lines"
)

# Only look at the package before the '->' arrow
# We do this by just replacing any text after an -> with nothing when doing the matching
awk '{line = $0; sub(/->.*/, "", line); if (match(line, /circuit\/backstack.*/) > 0) print}' circuit-runtime/src/androidMain/baseline-prof.txt >backstack/src/androidMain/baseline-prof.txt
awk '{line = $0; sub(/->.*/, "", line); if (match(line, /circuit\/foundation.*/) > 0) print}' circuit-runtime/src/androidMain/baseline-prof.txt >circuit-foundation/src/androidMain/baseline-prof.txt
awk '{line = $0; sub(/->.*/, "", line); if (match(line, /circuit\/overlay.*/) > 0) print}' circuit-runtime/src/androidMain/baseline-prof.txt >circuit-overlay/src/androidMain/baseline-prof.txt
awk '{line = $0; sub(/->.*/, "", line); if (match(line, /circuit\/retained.*/) > 0) print}' circuit-runtime/src/androidMain/baseline-prof.txt >circuit-retained/src/androidMain/baseline-prof.txt
awk '{line = $0; sub(/->.*/, "", line); if (match(line, /circuit\/runtime\/presenter.*/) > 0) print}' circuit-runtime/src/androidMain/baseline-prof.txt >circuit-runtime-presenter/src/androidMain/baseline-prof.txt
awk '{line = $0; sub(/->.*/, "", line); if (match(line, /circuit\/runtime\/ui.*/) > 0) print}' circuit-runtime/src/androidMain/baseline-prof.txt >circuit-runtime-ui/src/androidMain/baseline-prof.txt

# TODO why doesn't a loop below work??
#artifacts=(
#  "circuit\/backstack.*:backstack"
#  "circuit\/foundation.*:circuit-foundation"
#  "circuit\/overlay.*:circuit-overlay"
#  "circuit\/retained.*:circuit-retained"
#  "circuit\/runtime\/presenter.*:circuit-runtime-presenter"
#  "circuit\/runtime\/ui.*:circuit-runtime-ui"
#)

#for artifact in "${artifacts[@]}" ; do
#  regex=${artifact%%:*}
#  location=${artifact#*:}
#  target_file="$location"/src/androidMain/baseline-prof.txt
#  printf "Mapping '%s' to '%s'.\n" "$regex" "$target_file"
#  awk -v regexVar="$regex" '{line = $0; sub(/->.*/, "", line); if (match(line, /regexVar/) > 0) print }' circuit-runtime/src/androidMain/baseline-prof.txt >"$target_file"
#done

# Now finally remove any moved lines from the original source
removeLines backstack/src/androidMain/baseline-prof.txt circuit-runtime/src/androidMain/baseline-prof.txt
removeLines circuit-foundation/src/androidMain/baseline-prof.txt circuit-runtime/src/androidMain/baseline-prof.txt
removeLines circuit-overlay/src/androidMain/baseline-prof.txt circuit-runtime/src/androidMain/baseline-prof.txt
removeLines circuit-retained/src/androidMain/baseline-prof.txt circuit-runtime/src/androidMain/baseline-prof.txt
removeLines circuit-runtime-presenter/src/androidMain/baseline-prof.txt circuit-runtime/src/androidMain/baseline-prof.txt
removeLines circuit-runtime-ui/src/androidMain/baseline-prof.txt circuit-runtime/src/androidMain/baseline-prof.txt
