"""Tests for icons/scripts/fetch_icon.py (SteamGridDB) against a faked `requests`.

No test reaches the network or reads icons/.env: conftest.no_real_env leaves the key unset and
FakeHttp answers every request. Game names and URLs are made up.
"""
import sys

import pytest
import requests
from PIL import Image

import utils
from fakes import FakeHttp, FakeResponse, png_bytes

SEARCH = "https://www.steamgriddb.com/api/v2/search/autocomplete/"
ICONS = "https://www.steamgriddb.com/api/v2/icons/game/"


@pytest.fixture
def key(monkeypatch):
    monkeypatch.setenv("SGDB_API_KEY", "test-key")


def _icon(url, width, height):
    return {"url": url, "width": width, "height": height}


# ---------------------------------------------------------------------------------------------
# import / API key
# ---------------------------------------------------------------------------------------------


def test_import_does_not_load_the_env_file(monkeypatch):
    calls = []
    monkeypatch.setattr(utils, "load_env", lambda: calls.append(1))
    monkeypatch.setattr(utils, "_env_loaded", False)
    sys.modules.pop("fetch_icon", None)

    import fetch_icon  # noqa: F401

    assert calls == []


def test_api_key_is_read_at_call_time(fetch_icon, monkeypatch):
    monkeypatch.setenv("SGDB_API_KEY", "set-after-import")

    assert fetch_icon._api_key() == "set-after-import"


def test_api_key_loads_the_env_file_on_first_use(fetch_icon, monkeypatch):
    monkeypatch.setattr(utils, "_env_loaded", False)
    monkeypatch.setattr(utils, "load_env", lambda: monkeypatch.setenv("SGDB_API_KEY", "from-file"))

    assert fetch_icon._api_key() == "from-file"


def test_search_without_key_skips_the_request(fetch_icon, monkeypatch):
    http = FakeHttp(monkeypatch)

    assert fetch_icon.search_sgdb_by_text("Some Game") is None
    assert http.calls == []


# ---------------------------------------------------------------------------------------------
# _safe_json
# ---------------------------------------------------------------------------------------------


def test_safe_json_returns_the_parsed_body(fetch_icon):
    assert fetch_icon._safe_json(FakeResponse(json_data={"data": [1]})) == {"data": [1]}


def test_safe_json_treats_a_non_json_body_as_empty(fetch_icon):
    assert fetch_icon._safe_json(FakeResponse(status_code=429, json_error=True)) == {}


# ---------------------------------------------------------------------------------------------
# search_sgdb_by_text
# ---------------------------------------------------------------------------------------------


def test_search_returns_the_first_id_and_sends_the_key(fetch_icon, key, monkeypatch):
    http = FakeHttp(monkeypatch, {SEARCH: FakeResponse(json_data={"data": [{"id": 7}, {"id": 8}]})})

    assert fetch_icon.search_sgdb_by_text("Some Game") == 7
    method, url, kwargs = http.calls[0]
    assert url == SEARCH + "Some%20Game"
    assert kwargs["headers"] == {"Authorization": "Bearer test-key"}


def test_search_quotes_slashes_in_the_name(fetch_icon, key, monkeypatch):
    http = FakeHttp(monkeypatch, {SEARCH: FakeResponse(json_data={"data": []})})

    fetch_icon.search_sgdb_by_text("A/B")

    assert http.urls() == [SEARCH + "A%2FB"]


@pytest.mark.parametrize("answer", [
    FakeResponse(json_data={"data": []}),
    FakeResponse(json_data={}),
    FakeResponse(status_code=500, json_data={"data": [{"id": 1}]}),
    FakeResponse(json_error=True),
    requests.exceptions.ConnectionError("offline"),
])
def test_search_returns_none_when_nothing_usable_comes_back(fetch_icon, key, monkeypatch, answer):
    FakeHttp(monkeypatch, {SEARCH: answer})

    assert fetch_icon.search_sgdb_by_text("Some Game") is None


# ---------------------------------------------------------------------------------------------
# fetch_steamgriddb_icon (automatic)
# ---------------------------------------------------------------------------------------------


def test_auto_pick_skips_non_square_and_small_icons(fetch_icon, key, monkeypatch):
    FakeHttp(monkeypatch, {ICONS: FakeResponse(json_data={"data": [
        _icon("https://img/wide", 512, 256),     # ratio 0.5
        _icon("https://img/small", 128, 128),    # square but under 256
        _icon("https://img/unknown", 0, 0),      # no size
        _icon("https://img/good", 300, 256),     # ratio 0.85, both >= 256
        _icon("https://img/later", 512, 512),
    ]})})

    assert fetch_icon.fetch_steamgriddb_icon(1) == "https://img/good"


def test_auto_pick_accepts_exactly_80_percent_and_256(fetch_icon, key, monkeypatch):
    FakeHttp(monkeypatch, {ICONS: FakeResponse(json_data={"data": [_icon("https://img/edge", 320, 256)]})})

    assert fetch_icon.fetch_steamgriddb_icon(1) == "https://img/edge"


def test_auto_pick_returns_none_when_no_icon_qualifies(fetch_icon, key, monkeypatch):
    FakeHttp(monkeypatch, {ICONS: FakeResponse(json_data={"data": [_icon("https://img/wide", 512, 128)]})})

    assert fetch_icon.fetch_steamgriddb_icon(1) is None


@pytest.mark.parametrize("answer", [
    FakeResponse(status_code=404),
    FakeResponse(json_error=True),
    FakeResponse(json_data={"data": []}),
    requests.exceptions.Timeout("slow"),
])
def test_icon_list_failures_return_none(fetch_icon, key, monkeypatch, answer):
    FakeHttp(monkeypatch, {ICONS: answer})

    assert fetch_icon.fetch_steamgriddb_icon(1) is None


# ---------------------------------------------------------------------------------------------
# fetch_steamgriddb_icon (interactive)
# ---------------------------------------------------------------------------------------------


def _answers(monkeypatch, *replies):
    replies = list(replies)
    monkeypatch.setattr("builtins.input", lambda prompt="": replies.pop(0))
    return replies


def _six_icons():
    return {"data": [_icon(f"https://img/{i}", 64, 32) for i in range(1, 7)]}


def test_interactive_returns_the_chosen_icon_even_if_small(fetch_icon, key, monkeypatch):
    FakeHttp(monkeypatch, {ICONS: FakeResponse(json_data=_six_icons())})
    _answers(monkeypatch, "2")

    assert fetch_icon.fetch_steamgriddb_icon(1, interactive=True) == "https://img/2"


def test_interactive_reasks_on_invalid_input_and_offers_five_at_most(fetch_icon, key, monkeypatch):
    FakeHttp(monkeypatch, {ICONS: FakeResponse(json_data=_six_icons())})
    left = _answers(monkeypatch, "abc", "0", "6", " 5 ")

    assert fetch_icon.fetch_steamgriddb_icon(1, interactive=True) == "https://img/5"
    assert left == []


def test_interactive_skip_returns_none(fetch_icon, key, monkeypatch):
    FakeHttp(monkeypatch, {ICONS: FakeResponse(json_data=_six_icons())})
    _answers(monkeypatch, "S")

    assert fetch_icon.fetch_steamgriddb_icon(1, interactive=True) is None


# ---------------------------------------------------------------------------------------------
# fetch_sgdb_icon
# ---------------------------------------------------------------------------------------------


def test_fetch_sgdb_icon_downloads_the_picked_icon(fetch_icon, key, monkeypatch):
    http = FakeHttp(monkeypatch, {
        SEARCH: FakeResponse(json_data={"data": [{"id": 42}]}),
        ICONS: FakeResponse(json_data={"data": [_icon("https://img/good", 256, 256)]}),
        "https://img/good": FakeResponse(content=png_bytes(256, 256)),
    })

    image = fetch_icon.fetch_sgdb_icon("Some Game (Region) [!].zip")

    assert isinstance(image, Image.Image)
    assert image.mode == "RGBA"
    assert image.size == (256, 256)
    assert http.urls()[0] == SEARCH + "Some%20Game"
    assert http.urls()[1].startswith(ICONS + "42?")


def test_fetch_sgdb_icon_without_key_makes_no_request(fetch_icon, monkeypatch):
    http = FakeHttp(monkeypatch)

    assert fetch_icon.fetch_sgdb_icon("Some Game.aaa") is None
    assert http.calls == []


@pytest.mark.parametrize("download", [
    FakeResponse(status_code=404),
    FakeResponse(content=b"not an image"),
    requests.exceptions.ConnectionError("offline"),
])
def test_fetch_sgdb_icon_download_failures_return_none(fetch_icon, key, monkeypatch, download):
    FakeHttp(monkeypatch, {
        SEARCH: FakeResponse(json_data={"data": [{"id": 42}]}),
        ICONS: FakeResponse(json_data={"data": [_icon("https://img/good", 256, 256)]}),
        "https://img/good": download,
    })

    assert fetch_icon.fetch_sgdb_icon("Some Game.aaa") is None


def test_fetch_sgdb_icon_stops_when_the_game_is_not_found(fetch_icon, key, monkeypatch):
    http = FakeHttp(monkeypatch, {SEARCH: FakeResponse(json_data={"data": []})})

    assert fetch_icon.fetch_sgdb_icon("Some Game.aaa") is None
    assert len(http.calls) == 1


# ---------------------------------------------------------------------------------------------
# fetch_sgdb_multiple_icons
# ---------------------------------------------------------------------------------------------


def test_multiple_icons_keeps_squarish_ones_up_to_the_limit(fetch_icon, key, monkeypatch):
    icons = [_icon("https://img/wide", 400, 100)] + [_icon(f"https://img/{i}", 64, 64) for i in range(4)]
    http = FakeHttp(monkeypatch, {
        SEARCH: FakeResponse(json_data={"data": [{"id": 3}]}),
        ICONS: FakeResponse(json_data={"data": icons}),
        "https://img/": FakeResponse(content=png_bytes(64, 64)),
    })

    images = fetch_icon.fetch_sgdb_multiple_icons("Some Game.aaa", limit=3)

    assert len(images) == 3
    assert all(image.mode == "RGBA" for image in images)
    assert "https://img/wide" not in http.urls()


def test_multiple_icons_skips_broken_downloads(fetch_icon, key, monkeypatch):
    icons = [_icon(f"https://img/{name}", 64, 64) for name in ("ok1", "missing", "garbage", "offline", "ok2")]
    FakeHttp(monkeypatch, {
        SEARCH: FakeResponse(json_data={"data": [{"id": 3}]}),
        ICONS: FakeResponse(json_data={"data": icons}),
        "https://img/ok": FakeResponse(content=png_bytes(64, 64)),
        "https://img/missing": FakeResponse(status_code=404),
        "https://img/garbage": FakeResponse(content=b"not an image"),
        "https://img/offline": requests.exceptions.ConnectionError("offline"),
    })

    assert len(fetch_icon.fetch_sgdb_multiple_icons("Some Game.aaa")) == 2


def test_multiple_icons_empty_when_game_not_found_or_list_fails(fetch_icon, key, monkeypatch):
    FakeHttp(monkeypatch, {SEARCH: FakeResponse(json_data={"data": []})})
    assert fetch_icon.fetch_sgdb_multiple_icons("Some Game.aaa") == []

    FakeHttp(monkeypatch, {
        SEARCH: FakeResponse(json_data={"data": [{"id": 3}]}),
        ICONS: requests.exceptions.ConnectionError("offline"),
    })
    assert fetch_icon.fetch_sgdb_multiple_icons("Some Game.aaa") == []
