"""Makes icons/scripts importable the way the scripts import each other (`from utils import ...`)."""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "scripts"))

import pytest  # noqa: E402


@pytest.fixture
def master_icon(monkeypatch):
    """master_icon, imported without reading the real icons/.env.

    fetch_icon and fetch_smart call load_env() at import time; with utils.load_env replaced by a
    no-op first, the fresh import binds the no-op instead.
    """
    import utils

    monkeypatch.setattr(utils, "load_env", lambda: None)
    for name in ("master_icon", "fetch_icon", "fetch_smart"):
        sys.modules.pop(name, None)
    import master_icon as module

    return module
