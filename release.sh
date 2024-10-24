#!/usr/bin/env bash

set -exo pipefail

# Gets a property out of a .properties file
# usage: getProperty $key $filename
function getProperty() {
  grep "${1}" "$2" | cut -d'=' -f2
}

# Increments an input version string given a version type
# usage: increment_version $version $version_type
increment_version() {
  local delimiter=.
  local array=()
  while IFS='' read -r line; do array+=("$line"); done < <(echo "$1" | tr $delimiter '\n')
  local version_type=$2
  local major=${array[0]}
  local minor=${array[1]}
  local patch=${array[2]}

  if [ "$version_type" = "--major" ]; then
    major=$((major + 1))
    minor=0
    patch=0
  elif [ "$version_type" = "--minor" ]; then
    minor=$((minor + 1))
    patch=0
  elif [ "$version_type" = "--patch" ]; then
    patch=$((patch + 1))
  else
    echo "Invalid version type. Must be one of: '--major', '--minor', '--patch'"
    exit 1
  fi

  incremented_version="$major.$minor.$patch"

  echo "${incremented_version}"
}

# Gets the latest version from the CHANGELOG.md file. Note this assumes the changelog is updated with the
# new version as the latest, so it gets the *2nd* match.
# usage: get_latest_version $changelog_file
get_latest_version() {
  local changelog_file=$1
  grep -m 2 -o '^[0-9]\+\.[0-9]\+\.[0-9]\+' "$changelog_file" | tail -n 1
}

# Updates the VERSION_NAME prop in all gradle.properties files to a new value
# usage: update_gradle_properties $new_version
update_gradle_properties() {
  local new_version=$1

  find . -type f -name 'gradle.properties' | while read -r file; do
    if grep -q "VERSION_NAME=" "$file"; then
      local prev_version
      prev_version=$(getProperty 'VERSION_NAME' "${file}")
      sed -i '' "s/${prev_version}/${new_version}/g" "${file}"
    fi
  done
}

# default to patch if no second argument is given
version_type=${1:---patch}
LATEST_VERSION=$(get_latest_version CHANGELOG.md)
NEW_VERSION=$(increment_version "$LATEST_VERSION" "$version_type")
NEXT_SNAPSHOT_VERSION="$(increment_version "$NEW_VERSION" --minor)-SNAPSHOT"

echo "Publishing $NEW_VERSION"

# Prepare release
update_gradle_properties "$NEW_VERSION"
git commit -am "Prepare for release $NEW_VERSION."
git tag -a "$NEW_VERSION" -m "Version $NEW_VERSION"

# Publish
./gradlew publish --no-configuration-cache -PSONATYPE_CONNECT_TIMEOUT_SECONDS=300

# Prepare next snapshot
echo "Setting next snapshot version $NEXT_SNAPSHOT_VERSION"
update_gradle_properties "$NEXT_SNAPSHOT_VERSION"
git commit -am "Prepare next development version."

# Push it all up
git push && git push --tags

# Publish docs
./deploy_website.sh
