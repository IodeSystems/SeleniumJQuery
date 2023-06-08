#!/usr/bin/env bash
set -euo pipefail
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
SCRIPT_DIR_RELATIVE="$(dirname "$SCRIPT_SOURCE")"
SCRIPT_DIR_ABSOLUTE="$(cd "$SCRIPT_DIR_RELATIVE" && pwd)"
cd "$SCRIPT_DIR_ABSOLUTE"/../

PATH="$(pwd)"/bin:$PATH

unameOut="$(uname -a)"
case "${unameOut}" in
Linux*)
  SED_COMMAND="sed"
  ;;
Darwin*)
  SED_COMMAND="gsed"
  ;;
*) echo "Unsupported system: ${unameOut}" && exit 1 ;;
esac

# Maven 3 corrupts concurrent builds!
# If EXECUTOR_NUMBER is defined, we are running on jenkins, so set the maven
# repo to a local one. This is to avoid concurrent builds from overwriting each
# other's artifacts.
if [ -n "${EXECUTOR_NUMBER:-}" ]; then
  mkdir -p "$HOME/.m2/repo@$EXECUTOR_NUMBER"
  export MAVEN_OPTS="-Dmaven.repo.local=$HOME/.m2/repo@$EXECUTOR_NUMBER"
fi

export SED_COMMAND
