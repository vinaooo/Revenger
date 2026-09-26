"""Platform data for the icon scripts, loaded from icons/platforms.json.

The ids themselves (platforms, cores, file extensions) live only in that JSON file, like the
app's config.json and default_settings.json; the code here only looks them up.
"""
import json
import os

DEFAULT_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "platforms.json")

_cache = {}


def load(path=DEFAULT_PATH):
    """Returns the parsed platform data, reading each file once."""
    if path not in _cache:
        with open(path, encoding="utf-8") as f:
            _cache[path] = json.load(f)
    return _cache[path]


def determine_platform(core, rom, path=DEFAULT_PATH):
    """Platform id for a ROM: by extension first, then by core name, else "unknown".

    An extension listed under "ambiguous_extensions" is resolved by the first core substring that
    matches, falling back to that entry's default. Core names match by substring, in file order,
    after stripping the libretro suffixes.
    """
    data = load(path)
    ext = rom.split(".")[-1].lower() if "." in rom else ""

    ambiguous = data["ambiguous_extensions"].get(ext)
    if ambiguous:
        for substring, platform in ambiguous["by_core_substring"]:
            if substring in core:
                return platform
        return ambiguous["default"]

    platform = data["extension_to_platform"].get(ext)
    if platform:
        return platform

    core_clean = core.replace("_libretro_android", "").replace("_libretro", "")
    for key, platform in data["core_to_platform"]:
        if key in core_clean:
            return platform
    return "unknown"


def console_icon_file(platform, path=DEFAULT_PATH):
    """File name (in icons/images/) of the console illustration for a platform, or None."""
    return load(path)["console_icons"].get(platform)


def igdb_platform_id(platform, path=DEFAULT_PATH):
    """IGDB numeric platform id for a platform (case-insensitive), or None."""
    return load(path)["igdb_platform_ids"].get(platform.lower())
