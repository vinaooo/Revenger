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

python3 icons/scripts/master_icon.py --gui-web "$@"

echo "[Revenger] Selection complete!"