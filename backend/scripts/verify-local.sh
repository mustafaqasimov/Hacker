#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
exec "${MAVEN_HOME:?Set MAVEN_HOME}/bin/mvn" -B -ntp -Dmaven.repo.local="${MAVEN_REPOSITORY:-$HOME/.m2/repository}" clean verify
