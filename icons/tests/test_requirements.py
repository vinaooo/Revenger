"""Every third-party module the icon scripts import must be declared in icons/requirements.txt.

The scripts run under the system python3 (generateIcons) or a fresh venv (testScripts), so an
undeclared dependency only shows up as an ImportError on someone else's machine.
"""
import ast
import os
import re
import sys

ICONS_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCRIPTS_DIR = os.path.join(ICONS_DIR, "scripts")

# Import name -> distribution name, where they differ.
DISTRIBUTION_FOR_IMPORT = {"PIL": "Pillow"}


def _top_level_imports(path):
    tree = ast.parse(open(path, encoding="utf-8").read(), filename=path)
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                yield alias.name.split(".")[0]
        elif isinstance(node, ast.ImportFrom) and node.level == 0 and node.module:
            yield node.module.split(".")[0]


def _third_party_imports():
    local_modules = {name[:-3] for name in os.listdir(SCRIPTS_DIR) if name.endswith(".py")}
    found = set()
    for name in os.listdir(SCRIPTS_DIR):
        if name.endswith(".py"):
            found.update(_top_level_imports(os.path.join(SCRIPTS_DIR, name)))
    return {m for m in found if m not in local_modules and m not in sys.stdlib_module_names}


def _declared_distributions():
    declared = set()
    with open(os.path.join(ICONS_DIR, "requirements.txt"), encoding="utf-8") as f:
        for line in f:
            line = line.split("#", 1)[0].strip()
            if line:
                declared.add(re.split(r"[<>=!~\[; ]", line, maxsplit=1)[0].lower())
    return declared


def test_every_third_party_import_is_declared():
    declared = _declared_distributions()
    missing = sorted(
        DISTRIBUTION_FOR_IMPORT.get(m, m)
        for m in _third_party_imports()
        if DISTRIBUTION_FOR_IMPORT.get(m, m).lower() not in declared
    )

    assert missing == [], f"add to icons/requirements.txt: {missing}"


def test_the_scripts_do_import_third_party_modules():
    # Guards the scan itself: if it found nothing, the test above would pass vacuously.
    assert {"requests", "PIL"} <= _third_party_imports()
