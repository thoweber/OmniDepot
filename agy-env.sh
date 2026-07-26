#!/usr/bin/env bash
# OmniDepot AGY Launcher Script
# Automatically loads environment variables from agy.env and passes all arguments to agy

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/agy.env"

if [[ -f "${ENV_FILE}" ]]; then
    set -o allexport
    # Source agy.env file ignoring comments and empty lines
    source "${ENV_FILE}"
    set +o allexport
else
    echo "[WARN] agy.env file not found at ${ENV_FILE}. Copy agy.env.example to agy.env to configure local credentials." >&2
fi

exec agy "$@"
