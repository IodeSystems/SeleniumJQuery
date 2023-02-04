#!/usr/bin/env bash
set -euo pipefail
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
SCRIPT_DIR_RELATIVE="$(dirname "$SCRIPT_SOURCE")"
SCRIPT_DIR_ABSOLUTE="$(cd "$SCRIPT_DIR_RELATIVE" && pwd)"
cd "$SCRIPT_DIR_ABSOLUTE"/../
