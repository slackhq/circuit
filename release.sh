#!/usr/bin/env bash

set -exo pipefail

# Gets a property out of a .properties file
# usage: getProperty $key $filename
function getProperty() {
    grep "${1}" "$2" | cut -d'=' -f2
}

NEW_VERSION=$1
SNAPSHOT_VERSION=$(getProperty 'VERSION_NAME' gradle.properties)

echo "Publishing $NEW_VERSION"

# Prepare release
sed -i '' "s/${SNAPSHOT_VERSION}/${NEW_VERSION}/g" gradle.properties
git commit -am "Prepare for release $NEW_VERSION."
git tag -a "$NEW_VERSION" -m "Version $NEW_VERSION"

# Publish
./gradlew publish -x dokkaHtml

# Prepare next snapshot
echo "Restoring snapshot version $SNAPSHOT_VERSION"
sed -i '' "s/${NEW_VERSION}/${SNAPSHOT_VERSION}/g" gradle.properties
git commit -am "Prepare next development version."

# Push it all up
git push && git push --tags