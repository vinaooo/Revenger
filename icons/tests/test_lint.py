"""Keeps the icon scripts lint-clean with the repo's pinned ruff config (ruff.toml).

ruff is in icons/requirements-dev.txt, so `./gradlew testScripts` always runs this; a bare
`pytest` without the dev requirements skips it.
"""
import os
import shutil
import subprocess
import sys

import pytest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def _ruff_command():
    """ruff from this interpreter's environment if installed, else from PATH, else None."""
    ruff = os.path.join(os.path.dirname(sys.executable), "ruff")
    if os.path.exists(ruff):
        return ruff
    return shutil.which("ruff")


def test_icon_scripts_pass_ruff_with_the_repo_config():
    ruff = _ruff_command()
    if ruff is None:
        pytest.skip("ruff not installed (pip install -r icons/requirements-dev.txt)")

    result = subprocess.run(
        [ruff, "check", "--no-cache", "--config", os.path.join(REPO_ROOT, "ruff.toml"), "icons"],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
    )

    assert result.returncode == 0, result.stdout + result.stderr
