"""Tests for icons/platforms.json, the platforms module and the neutral console-icon files.

Platform, core and extension ids live only in icons/platforms.json. These tests read them from
there (or use made-up ids in a temp file) and never spell them out. That's the same rule as for the
app's config.json.
"""
import hashlib
import json
import os
import re

import pytest

import platforms

ICONS_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IMAGES_DIR = os.path.join(ICONS_DIR, "images")
REAL_DATA = platforms.load()

# SHA-256 of lowercase words that must never appear in a path or text file under icons/ again
# (the console images used to be named after them). Shared with the Kotlin naming guard.
DENYLIST_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "brand_denylist_sha256.txt")


def _load_denylist():
    with open(DENYLIST_FILE, encoding="utf-8") as f:
        return {line.strip() for line in f if line.strip() and not line.startswith("#")}


DENYLIST_SHA256 = _load_denylist()


# ---------------------------------------------------------------------------------------------
# The real data file and the image directory
# ---------------------------------------------------------------------------------------------


def test_every_image_has_a_neutral_name():
    bad = [n for n in os.listdir(IMAGES_DIR) if not re.fullmatch(r"platform_\d{3}\.png", n)]
    assert bad == []


def test_every_console_icon_entry_points_at_an_existing_image():
    missing = [f for f in REAL_DATA["console_icons"].values() if not os.path.isfile(os.path.join(IMAGES_DIR, f))]
    assert missing == []
    assert REAL_DATA["console_icons"], "the mapping should not be empty"


def test_every_platform_in_the_data_file_uses_a_known_shape():
    for ext, platform in REAL_DATA["extension_to_platform"].items():
        assert ext == ext.lower() and "." not in ext and platform
    for entry in REAL_DATA["core_to_platform"]:
        assert len(entry) == 2 and all(entry)
    for rule in REAL_DATA["ambiguous_extensions"].values():
        assert rule["default"] and all(len(pair) == 2 for pair in rule["by_core_substring"])
    assert all(isinstance(v, int) for v in REAL_DATA["igdb_platform_ids"].values())


def test_every_mapped_platform_loads_its_console_image(master_icon):
    for platform in REAL_DATA["console_icons"]:
        img = master_icon.fetch_console_fallback(platform)
        assert img is not None and img.mode == "RGBA" and img.size[0] > 0, platform


def test_fetch_console_fallback_returns_none_for_an_unknown_platform(master_icon):
    assert master_icon.fetch_console_fallback("not-a-platform") is None


def test_fetch_console_fallback_returns_none_when_the_mapped_file_is_missing(master_icon, monkeypatch):
    monkeypatch.setattr(platforms, "console_icon_file", lambda platform: "platform_999.png")
    assert master_icon.fetch_console_fallback("any") is None


def test_master_icon_determine_platform_delegates_to_the_data_file(master_icon):
    ext, platform = next(iter(REAL_DATA["extension_to_platform"].items()))
    assert master_icon.determine_platform("", f"Some Game.{ext}") == platform


def _tokens(text):
    return set(re.findall(r"[a-z0-9]+", text.lower()))


def test_the_denylist_file_holds_only_sha256_hashes():
    assert len(DENYLIST_SHA256) >= 36
    assert all(re.fullmatch(r"[0-9a-f]{64}", h) for h in DENYLIST_SHA256)


def test_no_brand_names_under_icons():
    hits = []
    for root, dirs, files in os.walk(ICONS_DIR):
        dirs[:] = [d for d in dirs if d not in ("__pycache__", ".cache", ".pytest_cache")]
        for name in files:
            if name == ".env":
                continue
            path = os.path.join(root, name)
            words = _tokens(os.path.relpath(path, ICONS_DIR))
            if not name.endswith((".png", ".ttf", ".pyc")):
                with open(path, encoding="utf-8", errors="ignore") as f:
                    words |= _tokens(f.read())
            if any(hashlib.sha256(w.encode()).hexdigest() in DENYLIST_SHA256 for w in words):
                hits.append(os.path.relpath(path, ICONS_DIR))
    assert hits == [], f"brand/manufacturer names found in: {hits}"


# ---------------------------------------------------------------------------------------------
# Lookup logic, on a made-up data file
# ---------------------------------------------------------------------------------------------


@pytest.fixture
def data_file(tmp_path):
    data = {
        "extension_to_platform": {"aaa": "platform-a", "bbb": "platform-b"},
        "ambiguous_extensions": {
            "amb": {"default": "platform-a", "by_core_substring": [["corex", "platform-x"], ["corey", "platform-y"]]}
        },
        "core_to_platform": [["alpha", "platform-alpha"], ["alphabet", "platform-never"], ["beta", "platform-beta"]],
        "console_icons": {"platform-a": "platform_001.png"},
        "igdb_platform_ids": {"platform-a": 7},
    }
    path = tmp_path / "platforms.json"
    path.write_text(json.dumps(data))
    return str(path)


def test_extension_decides_first(data_file):
    assert platforms.determine_platform("beta_libretro", "Some Game (Rev 1).aaa", data_file) == "platform-a"


def test_extension_match_ignores_case(data_file):
    assert platforms.determine_platform("", "Some Game.BBB", data_file) == "platform-b"


def test_ambiguous_extension_uses_the_first_matching_core_substring(data_file):
    assert platforms.determine_platform("my_corex_libretro", "g.amb", data_file) == "platform-x"
    assert platforms.determine_platform("corey_corex", "g.amb", data_file) == "platform-x"
    assert platforms.determine_platform("corey", "g.amb", data_file) == "platform-y"


def test_ambiguous_extension_falls_back_to_its_default(data_file):
    assert platforms.determine_platform("other", "g.amb", data_file) == "platform-a"


def test_unknown_extension_falls_back_to_the_core_after_stripping_libretro_suffixes(data_file):
    assert platforms.determine_platform("beta_libretro_android", "g.zzz", data_file) == "platform-beta"
    assert platforms.determine_platform("beta_libretro", "g", data_file) == "platform-beta"


def test_core_fallback_matches_substrings_in_file_order(data_file):
    # "alphabet" contains "alpha", which is listed first, so the later entry is never reached.
    assert platforms.determine_platform("alphabet", "g.zzz", data_file) == "platform-alpha"


def test_no_match_is_unknown(data_file):
    assert platforms.determine_platform("gamma", "g.zzz", data_file) == "unknown"
    assert platforms.determine_platform("", "", data_file) == "unknown"


def test_console_icon_file_lookup(data_file):
    assert platforms.console_icon_file("platform-a", data_file) == "platform_001.png"
    assert platforms.console_icon_file("platform-b", data_file) is None


def test_igdb_platform_id_lookup_ignores_case(data_file):
    assert platforms.igdb_platform_id("PLATFORM-A", data_file) == 7
    assert platforms.igdb_platform_id("platform-b", data_file) is None


def test_load_reads_each_file_once(data_file, monkeypatch):
    first = platforms.load(data_file)
    monkeypatch.setattr("builtins.open", lambda *a, **k: pytest.fail("read the file twice"))
    assert platforms.load(data_file) is first
