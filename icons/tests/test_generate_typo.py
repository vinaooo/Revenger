"""Tests for icons/scripts/generate_typo.py, the offline fallback icon (initials on a color)."""
import pytest

import generate_typo


@pytest.mark.parametrize(
    "name, initials",
    [
        ("Some Game", "SG"),
        ("some game", "SG"),
        ("Some Game Title Four", "SGT"),
        ("3 Little Words", "3LW"),
        ("Word", "W"),
        ("", "?"),
        ("!!! ???", "?"),
    ],
)
def test_extract_initials(name, initials):
    assert generate_typo.extract_initials(name) == initials


def test_stable_color_is_deterministic_and_from_the_palette():
    for name in ("Some Game", "Other Game", "A", ""):
        color = generate_typo.generate_stable_color(name)
        assert color == generate_typo.generate_stable_color(name)
        assert color in generate_typo.COLOR_PALETTE


def test_stable_color_spreads_over_the_palette():
    colors = {generate_typo.generate_stable_color(f"Game {i}") for i in range(50)}
    assert len(colors) > 3


def _name_with_color(index, wanted):
    for i in range(10_000):
        name = f"Game {i}"
        if (generate_typo.generate_stable_color(name) == generate_typo.COLOR_PALETTE[index]) == wanted:
            return name
    raise AssertionError("no such name")


def _count(img, rgba):
    return dict((color, n) for n, color in img.getcolors(maxcolors=img.width * img.height)).get(rgba, 0)


def test_icon_is_a_512_square_on_the_name_color_with_white_initials():
    name = _name_with_color(8, wanted=False)
    bg = generate_typo.generate_stable_color(name)

    icon = generate_typo.generate_typo_icon(f"{name} (Rev 1).zip")

    assert icon.size == (512, 512) and icon.mode == "RGBA"
    assert icon.getpixel((0, 0)) == (*bg, 255)
    assert _count(icon, (255, 255, 255, 255)) > 1000


def test_icon_uses_dark_initials_on_the_light_palette_entry():
    name = _name_with_color(8, wanted=True)

    icon = generate_typo.generate_typo_icon(name)

    assert _count(icon, (33, 33, 33, 255)) > 1000
    assert _count(icon, (255, 255, 255, 255)) == 0


def test_icon_color_comes_from_the_cleaned_rom_name():
    assert generate_typo.generate_typo_icon("Some Game (Rev 1) [!].zip").getpixel((0, 0)) == (
        *generate_typo.generate_stable_color("Some Game"),
        255,
    )


def test_icon_still_renders_without_the_bundled_font(monkeypatch):
    monkeypatch.setattr(generate_typo, "FONT_PATH", "/nonexistent/font.ttf")

    icon = generate_typo.generate_typo_icon("Some Game")

    assert icon.size == (512, 512)
