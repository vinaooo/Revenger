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

# SHA-256 of lowercase manufacturer/product words that must never appear in a path or text file
# under icons/ again (the console images used to be named after them). Stored as hashes so this
# file names nothing itself.
DENYLIST_SHA256 = {
    "26b6d4141119323d04c06e979f2a1eaafb214e225a9cbde510926b1b9cb8a2d5",
    "2ab6d53e717a0e0d773ccfdb8b0e84ac494729c6b567c72d256b883a9db17ea8",
    "2dae6772fc21a792a253c2192b6f659b2b5474422d08432d9edc37a6e3d327e3",
    "36e8072cf8b4a6b59338f43d3dfab4d99207e670c04bcfc4e0b2fa20518d061a",
    "3bb23652ba7f98e3ab857b88e9931eedbe766ec7ea1a18999cb11085fa8984f8",
    "3c7a4b6f9fdb74d9892cca93e119bfb25fd3f6ad2517d01ca9c4d87c6e617c8c",
    "429bcb40f738a0d79979a2ebcb3c170397eb4f1217619b3ab04c5ac63107dd27",
    "44cddc3f1be25271c8c2a824d85521ee8087db49c90efed6c5b884c995f4dd90",
    "49b4a588ebb75ec2aee09f85d17110d7f98e984a68604dc779ef84365fd39d59",
    "587d392b6c7680ec7656baf143511491e62c4ec6f459c21cce6953e6f1707bd7",
    "5abcd5f79571ea4a29d50a4ca577ac26468f1c3ce03b87667ae5d46554875bc2",
    "66fc6e52311f9fdd1bba7be21da06753de59c108ddcbc26236307d8587a60c4d",
    "68652da40a81880348e2a407e8f28fc76f76b7f54277b8eba55982ddbd7bbb6b",
    "72a8b03446cfacaa87d34cb55f20b1c11d077308c3dce6db82e7d9ca0c933108",
    "734cb1a7e57cbd48a1613e49379d7d0c949fa8afb026419b84d6dfbb4e2ee3f4",
    "84b30e651ef6388dd6fbb535a4d606094f256bbfee96a4365112b8e956e82dbf",
    "84f0ceca5ebebf54c45888a573b1c2380ec7e8b35289290af603644f04fb1e21",
    "8550a0638140d5a913df979c201c7d39e242182caf055b90df9c5a2d10c09e3c",
    "8bc0b6c18834c6ae24f81d2049ae3ad8e7859cf8f8f14ad75bf793bf15b40aa2",
    "956284d1a125b9708854cedbc0b86894d1365e0a076e3c53ddd9ef29ebd6be35",
    "9b7f5b1287ac54818ca570bcc3fc52715c674c1b46636962773e0c5e6946f1b9",
    "9d9b90817fc39231aedf8f19d5e19acde3e9421afff1018d942ecf9f2676176a",
    "a8fbe6e9a5b3b0a6cd75d1f744a6eaf9970893964cfd4d87790a2bded6276197",
    "ac080aad93da83ed84430165cb5ee7c024164e54ac839fde5a63c51f2fa9ff3d",
    "b40416491e0cfac117fcabd388d940a518814ce4906af1bfdcecc7d461449edc",
    "b6469853d5223da5c9c0c1058c77def4fc044253fef03603f8a49caca6d604dc",
    "c023b7b07742e120d8a6b04143594cac2ea04491b8f9f71fddd0ffd942bae1e5",
    "c0a4942143e872cd1ae29fc759e04526de2e909ac1732734d38550a29c2e2516",
    "c0e687a2d1fc85ee043de90c4bfafc300f927d5ee5439bd195ab9be96cacfae8",
    "cbdbac52f7c9a0f1930a725c65c1740a391664bd603e6f667a8eb8aacb8058e6",
    "cc9e0dc5cdfb515187fb6b9b96e7efe9a1daeecf636679fdb3bcd72bae581d2f",
    "d78269315503d6ebe9119bddf485915f6c847b8af005e1b69db1a2f9b987481f",
    "e8fcca0a0b80b19f127bf9757cdd779239ff57d53328fe500b0c7a244d9f0c1a",
    "ebadbf852026b96c78284656f89047f04ee9aaf3e712d111586dd08371a56b7c",
    "ec81dc4b9aa0d4544d95411d091e9478d0c75869085261deb2d69ec0bbc65ee2",
    "f3c1f895fdb1ac14c19166f4d606f9c8be3805ec2d4596dbf344d75a8c41e3a6",
}


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
