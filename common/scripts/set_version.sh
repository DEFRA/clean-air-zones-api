#!/usr/bin/env bash

NEW_VERSION=$1
if [ -z "$NEW_VERSION" ]; then
    echo "Usage: scripts/set_version.sh VERSION_TO_SET"
    exit 1
fi

./mvnw versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false