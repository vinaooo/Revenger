"""Tests for icons/scripts/fetch_smart.py (IGDB covers → smart icon) against a faked `requests`.

No test reaches the network or reads icons/.env: conftest.no_real_env leaves the credentials
unset and FakeHttp answers every request. The platform table lookup is stubbed with a made-up
platform, so no real platform id appears here.
"""
import sys

import pytest
import requests

import utils
from fakes import FakeHttp, FakeResponse, png_bytes

TOKEN = "https://id.twitch.tv/oauth2/token"
GAMES = "https://api.igdb.com/v4/games"
COVER = "https://images.example/"


@pytest.fixture
def creds(monkeypatch):
    monkeypatch.setenv("IGDB_CLIENT_ID", "client-id")
    monkeypatch.setenv("IGDB_CLIENT_SECRET", "client-secret")


@pytest.fixture(autouse=True)
def platform_table(fetch_smart, monkeypatch):
    """Only "platform-a" (IGDB id 99) is known."""
    monkeypatch.setattr(fetch_smart.platforms, "igdb_platform_id", {"platform-a": 99}.get)


def _game(name, cover_path=None):
    game = {"name": name}
    if cover_path:
        game["cover"] = {"url": f"//images.example/t_thumb/{cover_path}.jpg"}
    return game


def _cover_url(cover_path):
    return f"{COVER}t_1080p/{cover_path}.jpg"


def _token_ok():
    return FakeResponse(json_data={"access_token": "tok"})


# ---------------------------------------------------------------------------------------------
# import / credentials
# ---------------------------------------------------------------------------------------------


def test_import_does_not_load_the_env_file(monkeypatch):
    calls = []
    monkeypatch.setattr(utils, "load_env", lambda: calls.append(1))
    monkeypatch.setattr(utils, "_env_loaded", False)
    sys.modules.pop("fetch_smart", None)

    import fetch_smart  # noqa: F401

    assert calls == []


def test_credentials_are_read_at_call_time(fetch_smart, creds):
    assert fetch_smart._credentials() == ("client-id", "client-secret")


# ---------------------------------------------------------------------------------------------
# _safe_json
# ---------------------------------------------------------------------------------------------


def test_safe_json_returns_the_parsed_body(fetch_smart):
    assert fetch_smart._safe_json(FakeResponse(json_data=[1])) == [1]


def test_safe_json_treats_a_non_json_body_as_none(fetch_smart):
    assert fetch_smart._safe_json(FakeResponse(status_code=502, json_error=True)) is None


# ---------------------------------------------------------------------------------------------
# get_token
# ---------------------------------------------------------------------------------------------


def test_get_token_posts_the_credentials(fetch_smart, creds, monkeypatch):
    http = FakeHttp(monkeypatch, {TOKEN: _token_ok()})

    assert fetch_smart.get_token() == "tok"
    method, url, kwargs = http.calls[0]
    assert (method, url) == ("POST", TOKEN)
    assert kwargs["params"] == {
        "client_id": "client-id", "client_secret": "client-secret", "grant_type": "client_credentials",
    }


@pytest.mark.parametrize("missing", ["IGDB_CLIENT_ID", "IGDB_CLIENT_SECRET"])
def test_get_token_without_credentials_makes_no_request(fetch_smart, creds, monkeypatch, missing):
    monkeypatch.delenv(missing)
    http = FakeHttp(monkeypatch)

    assert fetch_smart.get_token() is None
    assert http.calls == []


@pytest.mark.parametrize("answer", [
    FakeResponse(status_code=401, json_data={"access_token": "tok"}),
    FakeResponse(json_error=True),
    FakeResponse(json_data={}),
    requests.exceptions.ConnectionError("offline"),
])
def test_get_token_failures_return_none(fetch_smart, creds, monkeypatch, answer):
    FakeHttp(monkeypatch, {TOKEN: answer})

    assert fetch_smart.get_token() is None


# ---------------------------------------------------------------------------------------------
# fetch_igdb_cover
# ---------------------------------------------------------------------------------------------


def test_cover_query_uses_platform_id_and_upgrades_the_thumbnail(fetch_smart, creds, monkeypatch):
    http = FakeHttp(monkeypatch, {GAMES: FakeResponse(json_data=[_game("Some Game", "c1")])})

    assert fetch_smart.fetch_igdb_cover("Some Game", "platform-a", "tok") == (_cover_url("c1"), "Some Game")
    _, _, kwargs = http.calls[0]
    assert kwargs["headers"] == {"Client-ID": "client-id", "Authorization": "Bearer tok"}
    assert kwargs["data"] == 'search "Some Game"; fields name, cover.url; where platforms = (99); limit 5;'


def test_cover_auto_only_looks_at_the_first_result(fetch_smart, creds, monkeypatch):
    FakeHttp(monkeypatch, {GAMES: FakeResponse(json_data=[_game("No Cover"), _game("Other", "c2")])})

    assert fetch_smart.fetch_igdb_cover("Some Game", "platform-a", "tok") == (None, None)


@pytest.mark.parametrize("answer", [
    FakeResponse(json_data=[]),
    FakeResponse(status_code=500, json_data=[_game("Some Game", "c1")]),
    FakeResponse(json_error=True),
    requests.exceptions.Timeout("slow"),
])
def test_cover_failures_return_none_pair(fetch_smart, creds, monkeypatch, answer):
    FakeHttp(monkeypatch, {GAMES: answer})

    assert fetch_smart.fetch_igdb_cover("Some Game", "platform-a", "tok") == (None, None)


def _answers(monkeypatch, *replies):
    replies = list(replies)
    monkeypatch.setattr("builtins.input", lambda prompt="": replies.pop(0))
    return replies


def test_cover_interactive_lists_only_games_with_a_cover(fetch_smart, creds, monkeypatch):
    FakeHttp(monkeypatch, {GAMES: FakeResponse(json_data=[
        _game("No Cover"), _game("First", "c1"), _game("Second", "c2"),
    ])})
    left = _answers(monkeypatch, "x", "3", "2")

    assert fetch_smart.fetch_igdb_cover("Some Game", "platform-a", "tok", interactive=True) == (
        _cover_url("c2"), "Second",
    )
    assert left == []


def test_cover_interactive_skip_returns_none_pair(fetch_smart, creds, monkeypatch):
    FakeHttp(monkeypatch, {GAMES: FakeResponse(json_data=[_game("First", "c1")])})
    _answers(monkeypatch, "s")

    assert fetch_smart.fetch_igdb_cover("Some Game", "platform-a", "tok", interactive=True) == (None, None)


def test_cover_interactive_without_covers_does_not_prompt(fetch_smart, creds, monkeypatch):
    FakeHttp(monkeypatch, {GAMES: FakeResponse(json_data=[_game("No Cover")])})
    _answers(monkeypatch)  # any prompt would pop from an empty list and fail

    assert fetch_smart.fetch_igdb_cover("Some Game", "platform-a", "tok", interactive=True) == (None, None)


# ---------------------------------------------------------------------------------------------
# generate_smart_icon_image
# ---------------------------------------------------------------------------------------------


@pytest.mark.parametrize("size", [(300, 400), (400, 300), (1024, 1024), (40, 60)])
def test_smart_icon_is_512_square_rgba(fetch_smart, size):
    image = fetch_smart.generate_smart_icon_image(png_bytes(*size))

    assert image.size == (512, 512)
    assert image.mode == "RGBA"


def test_smart_icon_centers_the_cover_over_a_darkened_background(fetch_smart):
    image = fetch_smart.generate_smart_icon_image(png_bytes(256, 512, (200, 40, 40, 255)))

    assert image.getpixel((256, 256)) == (200, 40, 40, 255)  # cover, untouched
    r, g, b, a = image.getpixel((10, 256))                     # blurred side band
    assert a == 255
    assert r < 200 and r > 100  # darkened by the ~30% black overlay


# ---------------------------------------------------------------------------------------------
# fetch_igdb_smart_icon
# ---------------------------------------------------------------------------------------------


def test_smart_icon_strips_tags_and_downloads_the_cover(fetch_smart, creds, monkeypatch):
    http = FakeHttp(monkeypatch, {
        TOKEN: _token_ok(),
        GAMES: FakeResponse(json_data=[_game("Some Game", "c1")]),
        COVER: FakeResponse(content=png_bytes(300, 400)),
    })

    image = fetch_smart.fetch_igdb_smart_icon("platform-a", "Some Game (Region) [!].zip")

    assert image.size == (512, 512)
    assert 'search "Some Game";' in http.calls[1][2]["data"]
    assert http.urls("GET") == [_cover_url("c1")]


def test_smart_icon_download_has_a_timeout(fetch_smart, creds, monkeypatch):
    """Regression: the cover download used to be requests.get(url) with no timeout, so a stalled
    server hung the icon step (and the Gradle build) forever. FakeHttp fails any call without one."""
    http = FakeHttp(monkeypatch, {
        TOKEN: _token_ok(),
        GAMES: FakeResponse(json_data=[_game("Some Game", "c1")]),
        COVER: FakeResponse(content=png_bytes(300, 400)),
    })

    fetch_smart.fetch_igdb_smart_icon("platform-a", "Some Game.zip")

    _, _, kwargs = http.calls[-1]
    assert kwargs["timeout"] > 0


def test_smart_icon_unknown_platform_makes_no_request(fetch_smart, creds, monkeypatch):
    http = FakeHttp(monkeypatch)

    assert fetch_smart.fetch_igdb_smart_icon("platform-z", "Some Game.zip") is None
    assert http.calls == []


@pytest.mark.parametrize("routes", [
    {TOKEN: FakeResponse(status_code=401)},
    {TOKEN: _token_ok(), GAMES: FakeResponse(json_data=[])},
    {TOKEN: _token_ok(), GAMES: FakeResponse(json_data=[_game("Some Game", "c1")]),
     COVER: FakeResponse(content=b"not an image")},
    {TOKEN: _token_ok(), GAMES: FakeResponse(json_data=[_game("Some Game", "c1")]),
     COVER: requests.exceptions.ConnectionError("offline")},
])
def test_smart_icon_failures_return_none(fetch_smart, creds, monkeypatch, routes):
    FakeHttp(monkeypatch, routes)

    assert fetch_smart.fetch_igdb_smart_icon("platform-a", "Some Game.zip") is None


# ---------------------------------------------------------------------------------------------
# fetch_igdb_multiple_covers
# ---------------------------------------------------------------------------------------------


def test_multiple_covers_builds_a_smart_icon_per_cover(fetch_smart, creds, monkeypatch):
    http = FakeHttp(monkeypatch, {
        TOKEN: _token_ok(),
        GAMES: FakeResponse(json_data=[_game("A", "c1"), _game("No Cover"), _game("B", "c2")]),
        COVER: FakeResponse(content=png_bytes(300, 400)),
    })

    images = fetch_smart.fetch_igdb_multiple_covers("platform-a", "Some Game (Region).zip", limit=3)

    assert [image.size for image in images] == [(512, 512), (512, 512)]
    assert http.calls[1][2]["data"].endswith("where platforms = (99); limit 3;")
    assert 'search "Some Game";' in http.calls[1][2]["data"]


def test_multiple_covers_skips_broken_downloads(fetch_smart, creds, monkeypatch):
    FakeHttp(monkeypatch, {
        TOKEN: _token_ok(),
        GAMES: FakeResponse(json_data=[_game(n, n) for n in ("ok1", "missing", "garbage", "offline", "ok2")]),
        COVER + "t_1080p/ok": FakeResponse(content=png_bytes(64, 64)),
        COVER + "t_1080p/missing": FakeResponse(status_code=404),
        COVER + "t_1080p/garbage": FakeResponse(content=b"not an image"),
        COVER + "t_1080p/offline": requests.exceptions.ConnectionError("offline"),
    })

    assert len(fetch_smart.fetch_igdb_multiple_covers("platform-a", "Some Game.zip")) == 2


@pytest.mark.parametrize("routes", [
    {TOKEN: FakeResponse(status_code=401)},
    {TOKEN: _token_ok(), GAMES: FakeResponse(json_error=True)},
    {TOKEN: _token_ok(), GAMES: requests.exceptions.ConnectionError("offline")},
])
def test_multiple_covers_failures_return_empty(fetch_smart, creds, monkeypatch, routes):
    FakeHttp(monkeypatch, routes)

    assert fetch_smart.fetch_igdb_multiple_covers("platform-a", "Some Game.zip") == []


def test_multiple_covers_unknown_platform_makes_no_request(fetch_smart, creds, monkeypatch):
    http = FakeHttp(monkeypatch)

    assert fetch_smart.fetch_igdb_multiple_covers("platform-z", "Some Game.zip") == []
    assert http.calls == []
