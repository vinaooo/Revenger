#!/bin/bash

# ==============================================================================
# Revenger Interactive Icon Studio
# ==============================================================================
# Executes parallel rendering of icons from all providers (SGDB, IGDB) 
# and dynamic generators, hosting a temporary local page for the user 
# to choose which image will become the definite one for the package.
# No unselected images are written to disk.

echo "[Revenger] Starting Icon Selection Module..."

if ! command -v python3 &> /dev/null
then
    echo "❌ Error: Python 3 not found on this machine."
    exit 1
fi

# The repo root is where this script lives. Resolve it instead of cd-ing there, so the script
# works from any directory and relative arguments (e.g. --config) still mean the caller's cwd.
repo_root="$(cd "$(dirname "$0")" && pwd)" || exit 1

python3 "$repo_root/icons/scripts/master_icon.py" --gui-web "$@"
status=$?
if [ "$status" -ne 0 ]; then
    echo "❌ Icon selection failed (exit $status)."
    exit "$status"
fi

echo "[Revenger] Selection complete!"
