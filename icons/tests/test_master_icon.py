"""Tests for the pure logic in icons/scripts/master_icon.py: config parsing, image helpers and
mipmap generation.

PROJECT_ROOT is pointed at a temp directory for every test, so nothing here reads the project's
real config or writes into app/src/main/res. All ids are made up (core-a, platform-a, .aaa).
"""
import json

import pytest
from PIL import Image


@pytest.fixture(autouse=True)
def temp_project(master_icon, monkeypatch, tmp_path):
    project = tmp_path / "project"
    project.mkdir()
    monkeypatch.setattr(master_icon, "PROJECT_ROOT", str(project))
    return project


def _write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(data if isinstance(data, str) else json.dumps(data))
    return path


@pytest.fixture
def assets(tmp_path):
    """A standalone assets tree: assets/config/config.json + assets/default_settings.json."""
    return tmp_path / "tree" / "assets"


PROFILES = [
    {"platform_id": "platform-a", "extensions": [".aaa"], "core": "core-a"},
    {"platform_id": "platform-b", "extensions": [".bbb"], "core": "core-b"},
    {"platform_id": "platform-c", "extensions": [".ccc"]},
]


# ---------------------------------------------------------------------------------------------
# parse_config_xml
# ---------------------------------------------------------------------------------------------


def test_manual_settings_take_the_core_from_the_config(master_icon, assets):
    config = _write(assets / "config" / "config.json", {"default_settings": False, "core": "core-x", "rom": "Some Game.aaa"})

    assert master_icon.parse_config_xml(str(config)) == ("core-x", "Some Game.aaa")


def test_default_settings_take_the_core_from_the_profile_matching_the_platform(master_icon, assets):
    _write(assets / "default_settings.json", PROFILES)
    config = _write(assets / "config" / "config.json", {"default_settings": True, "platform": "platform-b", "rom": "Some Game.aaa"})

    assert master_icon.parse_config_xml(str(config)) == ("core-b", "Some Game.aaa")


def test_default_settings_fall_back_to_the_rom_extension(master_icon, assets):
    _write(assets / "default_settings.json", PROFILES)
    config = _write(assets / "config" / "config.json", {"default_settings": True, "platform": "platform-z", "rom": "Some Game.AAA"})

    assert master_icon.parse_config_xml(str(config)) == ("core-a", "Some Game.AAA")


def test_default_settings_keep_the_config_core_when_the_profile_has_none(master_icon, assets):
    _write(assets / "default_settings.json", PROFILES)
    config = _write(assets / "config" / "config.json", {"default_settings": True, "platform": "platform-c", "core": "core-x", "rom": "g.ccc"})

    assert master_icon.parse_config_xml(str(config)) == ("core-x", "g.ccc")


def test_default_settings_without_any_matching_profile_keep_the_config_core(master_icon, assets):
    _write(assets / "default_settings.json", PROFILES)
    config = _write(assets / "config" / "config.json", {"default_settings": True, "platform": "platform-z", "rom": "g.zzz"})

    assert master_icon.parse_config_xml(str(config)) == ("", "g.zzz")


def test_config_manual_is_merged_over_the_config(master_icon, assets):
    config = _write(assets / "config" / "config.json", {"default_settings": False, "core": "core-x", "rom": "g.aaa"})
    _write(assets / "config" / "config_manual.json", {"core": "core-manual"})

    assert master_icon.parse_config_xml(str(config)) == ("core-manual", "g.aaa")


def test_values_are_stripped(master_icon, assets):
    config = _write(assets / "config" / "config.json", {"core": "  core-x ", "rom": " g.aaa  "})

    assert master_icon.parse_config_xml(str(config)) == ("core-x", "g.aaa")


def test_missing_config_gives_empty_values(master_icon, tmp_path):
    assert master_icon.parse_config_xml(str(tmp_path / "nope" / "config.json")) == ("", "")


def test_malformed_config_gives_empty_values(master_icon, assets):
    config = _write(assets / "config" / "config.json", "{not json")

    assert master_icon.parse_config_xml(str(config)) == ("", "")


def test_explicit_default_settings_path_wins_over_the_sibling_file(master_icon, assets, tmp_path):
    _write(assets / "default_settings.json", PROFILES)
    other = _write(tmp_path / "elsewhere" / "default_settings.json", [{"platform_id": "platform-a", "core": "core-other"}])
    config = _write(assets / "config" / "config.json", {"default_settings": True, "platform": "platform-a", "rom": "g.aaa"})

    assert master_icon.parse_config_xml(str(config), str(other)) == ("core-other", "g.aaa")


def test_without_a_sibling_file_the_project_default_settings_are_used(master_icon, assets, temp_project):
    _write(temp_project / "app" / "src" / "main" / "assets" / "default_settings.json", PROFILES)
    config = _write(assets / "config" / "config.json", {"default_settings": True, "platform": "platform-a", "rom": "g"})

    assert master_icon.parse_config_xml(str(config)) == ("core-a", "g")


def test_without_a_path_the_project_config_is_read(master_icon, temp_project):
    _write(temp_project / "app" / "src" / "main" / "assets" / "config" / "config.json", {"core": "core-p", "rom": "p.aaa"})

    assert master_icon.parse_config_xml() == ("core-p", "p.aaa")


# ---------------------------------------------------------------------------------------------
# Image helpers
# ---------------------------------------------------------------------------------------------

RED = (200, 10, 10, 255)


def test_make_perfect_square_returns_a_square_image_unchanged(master_icon):
    img = Image.new("RGBA", (64, 64), RED)

    assert master_icon.make_perfect_square(img) is img


@pytest.mark.parametrize("size", [(100, 50), (50, 100)])
def test_make_perfect_square_pads_to_the_longer_side_with_the_original_centered(master_icon, size):
    img = Image.new("RGBA", size, RED)

    square = master_icon.make_perfect_square(img)

    assert square.size == (100, 100)
    assert square.getpixel((50, 50)) == RED


def test_make_squircle_image_clears_the_corners_only(master_icon):
    out = master_icon.make_squircle_image(Image.new("RGBA", (100, 100), RED))

    assert out.getpixel((0, 0))[3] == 0
    assert out.getpixel((99, 99))[3] == 0
    assert out.getpixel((50, 50)) == RED
    assert out.getpixel((50, 0))[3] == 255


def test_make_round_image_keeps_only_the_circle(master_icon):
    out = master_icon.make_round_image(Image.new("RGBA", (100, 100), RED))

    assert out.getpixel((0, 0))[3] == 0
    assert out.getpixel((10, 10))[3] == 0
    assert out.getpixel((50, 50)) == RED


# ---------------------------------------------------------------------------------------------
# generate_android_icons
# ---------------------------------------------------------------------------------------------

LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def test_generate_android_icons_writes_every_density_at_the_right_sizes(master_icon, tmp_path):
    res = tmp_path / "res"

    master_icon.generate_android_icons(Image.new("RGBA", (300, 200), RED), res_dir=str(res))

    for density, size in LEGACY_SIZES.items():
        folder = res / f"mipmap-{density}"
        adaptive = int(size * 2.25)
        expected = {
            "ic_launcher.png": size,
            "ic_launcher_round.png": size,
            "ic_launcher_foreground.png": adaptive,
            "ic_launcher_background.png": adaptive,
        }
        assert sorted(p.name for p in folder.iterdir()) == sorted(expected)
        for name, px in expected.items():
            with Image.open(folder / name) as img:
                assert img.size == (px, px), f"{density}/{name}"
                assert img.mode == "RGBA"


def test_generate_android_icons_keeps_the_foreground_inside_the_safe_zone(master_icon, tmp_path):
    res = tmp_path / "res"

    master_icon.generate_android_icons(Image.new("RGBA", (100, 100), RED), res_dir=str(res))

    with Image.open(res / "mipmap-xxxhdpi" / "ic_launcher_foreground.png") as fg:
        assert fg.getpixel((0, 0))[3] == 0
        assert fg.getpixel((fg.width // 2, fg.height // 2))[3] == 255


def test_generate_android_icons_writes_both_adaptive_icon_xmls(master_icon, tmp_path):
    res = tmp_path / "res"

    master_icon.generate_android_icons(Image.new("RGBA", (64, 64), RED), res_dir=str(res))

    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        xml = (res / "mipmap-anydpi-v26" / name).read_text()
        assert "@mipmap/ic_launcher_background" in xml
        assert "@mipmap/ic_launcher_foreground" in xml


def test_generate_android_icons_defaults_to_the_project_res(master_icon, temp_project):
    master_icon.generate_android_icons(Image.new("RGBA", (64, 64), RED))

    assert (temp_project / "app" / "src" / "main" / "res" / "mipmap-mdpi" / "ic_launcher.png").is_file()
