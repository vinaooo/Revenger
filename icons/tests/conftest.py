"""Makes icons/scripts importable the way the scripts import each other (`from utils import ...`)."""
import importlib
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "scripts"))

import pytest  # noqa: E402


API_KEY_VARS = ("SGDB_API_KEY", "IGDB_CLIENT_ID", "IGDB_CLIENT_SECRET")


@pytest.fixture(autouse=True)
def no_real_env(monkeypatch):
    """Keeps every test away from the real icons/.env and its API keys.

    The fetchers read their keys through utils.ensure_env_loaded(); marking the env as already
    loaded makes that a no-op, and the key variables start unset. Tests that need a key set it
    with monkeypatch.setenv.
    """
    import utils

    monkeypatch.setattr(utils, "_env_loaded", True)
    for var in API_KEY_VARS:
        monkeypatch.delenv(var, raising=False)


def _fresh_import(name):
    """Imports `name` again, so each test gets module state untouched by earlier tests."""
    sys.modules.pop(name, None)
    return importlib.import_module(name)


@pytest.fixture
def master_icon():
    """A fresh master_icon, with fresh fetch_icon and fetch_smart behind it."""
    for name in ("fetch_icon", "fetch_smart"):
        sys.modules.pop(name, None)
    return _fresh_import("master_icon")


@pytest.fixture
def fetch_icon():
    return _fresh_import("fetch_icon")


@pytest.fixture
def fetch_smart():
    return _fresh_import("fetch_smart")
