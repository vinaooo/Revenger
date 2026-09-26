"""Tests for master_icon.main(): the method cascade, --force/--skip-downloads, the web-picker
override lock and the exit status.

Every generator is replaced by a recorder, PROJECT_ROOT points at a temp directory (so the lock
lives in tmp/icons/.cache) and generate_android_icons only records its image: nothing touches the
network, the real config or app/src/main/res. Ids are made up.
"""
import sys

import pytest
from PIL import Image

import web_gui

ROM = "Some Game.aaa"
METHODS = ("sgdb", "igdb", "console", "typo")


def _image(color):
    return Image.new("RGBA", (64, 64), color)


class Harness:
    """Records which generators main() called and which image reached the mipmap step."""

    def __init__(self, module, monkeypatch, tmp_path):
        self.module = module
        self.monkeypatch = monkeypatch
        self.calls = []
        self.results = {name: None for name in METHODS}
        self.generated = []
        self.cache = tmp_path / "icons" / ".cache"

        monkeypatch.setattr(module, "PROJECT_ROOT", str(tmp_path))
        monkeypatch.setattr(module, "parse_config_xml", lambda path=None: ("core-a", ROM))
        monkeypatch.setattr(module, "determine_platform", lambda core, rom: "platform-a")
        monkeypatch.setattr(module, "generate_android_icons", lambda img: self.generated.append(img))
        monkeypatch.setattr(module, "fetch_sgdb_icon", self._recorder("sgdb"))
        monkeypatch.setattr(module, "fetch_igdb_smart_icon", self._recorder("igdb"))
        monkeypatch.setattr(module, "fetch_console_fallback", self._recorder("console"))
        monkeypatch.setattr(module, "generate_typo_icon", self._recorder("typo"))

    def _recorder(self, name):
        def record(*args, **kwargs):
            self.calls.append(name)
            return self.results[name]

        return record

    def run(self, *argv):
        self.monkeypatch.setattr(sys, "argv", ["master_icon.py", *argv])
        self.module.main()

    def lock(self, rom, color=(1, 2, 3, 255)):
        self.cache.mkdir(parents=True)
        _image(color).save(self.cache / "custom_override_icon.png")
        (self.cache / "last_rom.txt").write_text(rom)


@pytest.fixture
def h(master_icon, monkeypatch, tmp_path):
    return Harness(master_icon, monkeypatch, tmp_path)


# ---------------------------------------------------------------------------------------------
# cascade
# ---------------------------------------------------------------------------------------------


@pytest.mark.parametrize("winner, expected_calls", [
    ("sgdb", ["sgdb"]),
    ("igdb", ["sgdb", "igdb"]),
    ("console", ["sgdb", "igdb", "console"]),
    ("typo", ["sgdb", "igdb", "console", "typo"]),
])
def test_cascade_stops_at_the_first_method_that_returns_an_image(h, winner, expected_calls):
    image = _image((9, 9, 9, 255))
    h.results[winner] = image

    h.run()

    assert h.calls == expected_calls
    assert h.generated == [image]


def test_skip_downloads_starts_at_the_console_fallback(h):
    h.results["sgdb"] = _image((1, 1, 1, 255))
    h.results["typo"] = _image((2, 2, 2, 255))

    h.run("--skip-downloads")

    assert h.calls == ["console", "typo"]
    assert h.generated == [h.results["typo"]]


@pytest.mark.parametrize("force, method", list(enumerate(METHODS, start=1)))
def test_force_runs_only_that_method(h, force, method):
    for name in METHODS:
        h.results[name] = _image((1, 1, 1, 255))

    h.run("--force", str(force))

    assert h.calls == [method]
    assert h.generated == [h.results[method]]


def test_all_methods_failing_exits_non_zero(h):
    with pytest.raises(SystemExit) as exit_info:
        h.run()

    assert exit_info.value.code not in (0, None)
    assert h.calls == list(METHODS)
    assert h.generated == []


def test_forced_method_failing_exits_non_zero(h):
    with pytest.raises(SystemExit) as exit_info:
        h.run("--force", "1")

    assert exit_info.value.code not in (0, None)


def test_typography_fallback_alone_succeeds(h):
    h.results["typo"] = _image((5, 5, 5, 255))

    h.run()  # no SystemExit: exit status 0

    assert h.generated == [h.results["typo"]]


@pytest.mark.parametrize("config", [(None, ROM), ("core-a", None), ("", "")])
def test_missing_core_or_rom_exits_1(h, monkeypatch, config):
    monkeypatch.setattr(h.module, "parse_config_xml", lambda path=None: config)

    with pytest.raises(SystemExit) as exit_info:
        h.run()

    assert exit_info.value.code == 1
    assert h.calls == []


def test_config_argument_is_passed_through(h, monkeypatch):
    seen = []
    monkeypatch.setattr(h.module, "parse_config_xml", lambda path=None: seen.append(path) or ("core-a", ROM))
    h.results["typo"] = _image((5, 5, 5, 255))

    h.run("--config", "/some/config.json")

    assert seen == ["/some/config.json"]


# ---------------------------------------------------------------------------------------------
# override lock
# ---------------------------------------------------------------------------------------------


def test_lock_for_the_same_rom_uses_the_override_and_skips_every_method(h):
    h.lock(ROM, color=(10, 20, 30, 255))

    h.run()

    assert h.calls == []
    assert len(h.generated) == 1
    assert h.generated[0].getpixel((0, 0)) == (10, 20, 30, 255)


def test_lock_for_another_rom_is_dropped_and_the_cascade_runs(h):
    h.lock("Other Game.aaa")
    h.results["sgdb"] = _image((1, 1, 1, 255))

    h.run()

    assert not (h.cache / "custom_override_icon.png").exists()
    assert not (h.cache / "last_rom.txt").exists()
    assert h.calls == ["sgdb"]


def test_override_image_without_lock_file_is_ignored(h):
    h.cache.mkdir(parents=True)
    _image((1, 2, 3, 255)).save(h.cache / "custom_override_icon.png")
    h.results["sgdb"] = _image((4, 4, 4, 255))

    h.run()

    assert h.generated == [h.results["sgdb"]]


# ---------------------------------------------------------------------------------------------
# --gui-web
# ---------------------------------------------------------------------------------------------


@pytest.fixture
def picker(h, monkeypatch):
    """Stubs the multi-result fetchers and the web picker; `picker.answer` is what it returns."""

    class Picker:
        answer = None
        context = None

    def start(context):
        Picker.context = context
        return Picker.answer

    monkeypatch.setattr(h.module, "fetch_sgdb_multiple_icons", lambda rom, limit=5: [_image((1, 0, 0, 255))])
    monkeypatch.setattr(h.module, "fetch_igdb_multiple_covers", lambda platform, rom, limit=5: [])
    monkeypatch.setattr(web_gui, "start_web_picker", start)
    h.results["console"] = Image.new("RGBA", (32, 16), (0, 1, 0, 255))
    h.results["typo"] = _image((0, 0, 1, 255))
    return Picker


def test_gui_web_shows_every_option_squared(h, picker):
    picker.answer = "CANCEL"

    with pytest.raises(SystemExit):
        h.run("--gui-web")

    assert [img.size for img in picker.context["sgdb"]] == [(64, 64)]
    assert picker.context["igdb"] == []
    width, height = picker.context["console"].size
    assert width == height
    assert picker.context["typo"] is not None


def test_gui_web_cancel_exits_0_and_changes_nothing(h, picker):
    picker.answer = "CANCEL"
    h.lock(ROM)

    with pytest.raises(SystemExit) as exit_info:
        h.run("--gui-web")

    assert exit_info.value.code == 0
    assert h.generated == []
    assert (h.cache / "last_rom.txt").exists()


def test_gui_web_choice_is_saved_as_the_lock_for_this_rom(h, picker):
    picker.answer = _image((7, 8, 9, 255))

    h.run("--gui-web")

    assert h.generated == [picker.answer]
    assert (h.cache / "last_rom.txt").read_text() == ROM
    with Image.open(h.cache / "custom_override_icon.png") as saved:
        assert saved.convert("RGBA").getpixel((0, 0)) == (7, 8, 9, 255)


def test_gui_web_ignores_an_existing_lock(h, picker):
    h.lock(ROM, color=(1, 1, 1, 255))
    picker.answer = _image((7, 8, 9, 255))

    h.run("--gui-web")

    assert h.generated == [picker.answer]


def test_gui_web_clear_override_removes_the_lock_and_runs_the_cascade(h, picker):
    h.lock(ROM)
    picker.answer = "CLEAR_OVERRIDE"
    h.results["sgdb"] = _image((3, 3, 3, 255))
    h.calls.clear()

    h.run("--gui-web")

    assert not (h.cache / "custom_override_icon.png").exists()
    assert not (h.cache / "last_rom.txt").exists()
    assert h.calls[-1] == "sgdb"  # after the picker's console/typo previews
    assert h.generated == [h.results["sgdb"]]
