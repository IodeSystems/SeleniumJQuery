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

export SED_COMMAND
