"""Tests for the pure helpers in icons/scripts/utils.py.

Run with: python3 -m pytest icons/tests (after pip install -r icons/requirements-dev.txt).

load_env() resolves its .env from utils.__file__, and the real icons/.env holds API keys, so every
load_env test points __file__ at a temporary directory and never reads the real file. Keys are
registered with monkeypatch first so os.environ is restored after each test.
"""
import os

import pytest

import utils

# ---------------------------------------------------------------------------------------------
# clean_rom_name
# ---------------------------------------------------------------------------------------------

EXTENSIONS = [
    "iso", "zip", "7z", "rar", "sfc", "smc", "gba", "gbc", "gb", "nes", "n64", "z64", "v64",
    "rvz", "chd", "bin", "cue", "gcm", "apk", "sms", "3ds", "md", "gen",
]


@pytest.mark.parametrize("ext", EXTENSIONS)
def test_clean_rom_name_strips_every_known_extension(ext):
    assert utils.clean_rom_name(f"Some Game.{ext}") == "Some Game"


def test_clean_rom_name_extension_match_ignores_case():
    assert utils.clean_rom_name("Some Game.ZIP") == "Some Game"


def test_clean_rom_name_keeps_unknown_extensions():
    assert utils.clean_rom_name("Some Game.txt") == "Some Game.txt"


def test_clean_rom_name_strips_only_the_final_extension():
    assert utils.clean_rom_name("Some.Game.nes") == "Some.Game"


def test_clean_rom_name_removes_parenthesized_and_bracketed_tags():
    assert utils.clean_rom_name("Some Game (Region) (Rev 1) [!].zip") == "Some Game"


def test_clean_rom_name_removes_tags_anywhere_in_the_name():
    assert utils.clean_rom_name("Some (Beta) Game [T+Lang].sfc") == "Some  Game"


def test_clean_rom_name_turns_spaced_dash_into_colon_subtitle():
    assert utils.clean_rom_name("Some Game - Part Two (Rev 1).gba") == "Some Game: Part Two"


def test_clean_rom_name_keeps_unspaced_dashes():
    assert utils.clean_rom_name("Some-Game.nes") == "Some-Game"


def test_clean_rom_name_trims_surrounding_whitespace():
    assert utils.clean_rom_name("  Some Game  ") == "Some Game"


def test_clean_rom_name_of_empty_string_is_empty():
    assert utils.clean_rom_name("") == ""


# ---------------------------------------------------------------------------------------------
# load_env
# ---------------------------------------------------------------------------------------------

KEYS = ["REVENGER_TEST_A", "REVENGER_TEST_B", "REVENGER_TEST_C", "REVENGER_TEST_D"]


@pytest.fixture
def env_dir(tmp_path, monkeypatch):
    """Points utils at tmp_path/scripts/utils.py, so load_env() reads tmp_path/.env."""
    scripts = tmp_path / "scripts"
    scripts.mkdir()
    monkeypatch.setattr(utils, "__file__", str(scripts / "utils.py"))
    for key in KEYS:
        monkeypatch.delenv(key, raising=False)
    return tmp_path


def test_load_env_reads_key_value_pairs(env_dir):
    (env_dir / ".env").write_text("REVENGER_TEST_A=first\nREVENGER_TEST_B=second\n")

    utils.load_env()

    assert os.environ["REVENGER_TEST_A"] == "first"
    assert os.environ["REVENGER_TEST_B"] == "second"


def test_load_env_strips_whitespace_and_quotes(env_dir):
    (env_dir / ".env").write_text(
        "  REVENGER_TEST_A =  spaced  \nREVENGER_TEST_B='single'\nREVENGER_TEST_C=\"double\"\n"
    )

    utils.load_env()

    assert os.environ["REVENGER_TEST_A"] == "spaced"
    assert os.environ["REVENGER_TEST_B"] == "single"
    assert os.environ["REVENGER_TEST_C"] == "double"


def test_load_env_splits_on_the_first_equals_only(env_dir):
    (env_dir / ".env").write_text("REVENGER_TEST_A=a=b==c\n")

    utils.load_env()

    assert os.environ["REVENGER_TEST_A"] == "a=b==c"


def test_load_env_skips_comments_blank_lines_and_lines_without_equals(env_dir):
    (env_dir / ".env").write_text(
        "# REVENGER_TEST_A=commented\n\n   \nREVENGER_TEST_B\nREVENGER_TEST_C=kept\n"
    )

    utils.load_env()

    assert "REVENGER_TEST_A" not in os.environ
    assert "REVENGER_TEST_B" not in os.environ
    assert os.environ["REVENGER_TEST_C"] == "kept"


def test_load_env_overrides_existing_variables(env_dir, monkeypatch):
    monkeypatch.setenv("REVENGER_TEST_A", "from-shell")
    (env_dir / ".env").write_text("REVENGER_TEST_A=from-file\n")

    utils.load_env()

    assert os.environ["REVENGER_TEST_A"] == "from-file"


def test_load_env_without_env_file_changes_nothing(env_dir):
    before = dict(os.environ)

    utils.load_env()

    assert dict(os.environ) == before
