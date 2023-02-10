#!/usr/bin/env sh

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

awk '/circuit\/overlay/' circuit/src/androidMain/baseline-prof.txt >circuit-overlay/src/androidMain/baseline-prof.txt
awk '/circuit\/retained/' circuit/src/androidMain/baseline-prof.txt >circuit-retained/src/androidMain/baseline-prof.txt
# Because backstack is used in circuit itself, only look at the package before the '->' arrow
awk '/circuit\/backstack.*->/' circuit/src/androidMain/baseline-prof.txt >backstack/src/androidMain/baseline-prof.txt

# Now finally remove any moved lines from the original source
removeLines circuit-overlay/src/androidMain/baseline-prof.txt circuit/src/androidMain/baseline-prof.txt
removeLines circuit-retained/src/androidMain/baseline-prof.txt circuit/src/androidMain/baseline-prof.txt
removeLines backstack/src/androidMain/baseline-prof.txt circuit/src/androidMain/baseline-prof.txt
