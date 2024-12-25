Releasing
=========

1. Update the `CHANGELOG.md` for the impending release.
2. Regenerate baseline profiles by running `./scripts/update-baseline-profiles.sh`. Note this step can take up to 15min.
   - If GMDs give you trouble, you can also connect a physical device and run `./gradlew generateBaselineProfile -Pcircuit.benchmark.useConnectedDevice=true`.
3. Run `./release.sh (--patch|--minor|--major)`.
4. Publish the release on the repo's releases tab.
