"""Tests for icons/scripts/web_gui.py, the browser icon picker.

The server tests run the real picker on 127.0.0.1 in this process: webbrowser.open is replaced by
a fake "browser" that talks to the server over loopback, so no browser opens and nothing leaves
the machine.
"""
import base64
import http.client
import json
import threading
from io import BytesIO
from urllib.parse import urlparse

import pytest
from PIL import Image

import web_gui


def _image(color, size=(64, 64)):
    return Image.new("RGBA", size, color)


def _context():
    return {
        "sgdb": [_image((255, 0, 0, 255)), _image((0, 255, 0, 255))],
        "igdb": [_image((0, 0, 255, 255), size=(512, 512))],
        "console": _image((10, 10, 10, 255)),
        "typo": _image((20, 20, 20, 255)),
    }


def _request(url, method="GET", body=None):
    parsed = urlparse(url)
    connection = http.client.HTTPConnection(parsed.hostname, parsed.port, timeout=5)
    try:
        payload = json.dumps(body).encode() if body is not None else None
        headers = {"Content-Type": "application/json"} if payload else {}
        connection.request(method, parsed.path or "/", body=payload, headers=headers)
        response = connection.getresponse()
        return response.status, response.read().decode()
    finally:
        connection.close()


def _run_picker(monkeypatch, context, browse):
    """Runs start_web_picker(context); the fake browser calls browse(url) on its own thread.

    Returns (picker result, urls the browser was asked to open, whatever browse returned).
    """
    opened, seen, browsers = [], {}, []

    def fake_open(url):
        opened.append(url)
        browser = threading.Thread(target=lambda: seen.setdefault("value", browse(url)), daemon=True)
        browsers.append(browser)
        browser.start()
        return True

    monkeypatch.setattr(web_gui.webbrowser, "open", fake_open)
    result_box = {}
    runner = threading.Thread(target=lambda: result_box.setdefault("result", web_gui.start_web_picker(context)), daemon=True)
    runner.start()
    runner.join(timeout=10)
    assert not runner.is_alive(), "the picker did not shut down after the selection"
    for browser in browsers:  # the server can shut down before the browser has read its reply
        browser.join(timeout=10)
    return result_box["result"], opened, seen.get("value")


# ---------------------------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------------------------


def test_image_to_base64_of_none_is_empty():
    assert web_gui.image_to_base64(None) == ""


def test_image_to_base64_is_a_png_thumbnail_of_at_most_256_px():
    original = _image((1, 2, 3, 255), size=(1024, 512))

    encoded = web_gui.image_to_base64(original)

    with Image.open(BytesIO(base64.b64decode(encoded))) as thumb:
        assert thumb.format == "PNG"
        assert thumb.size == (256, 128)
    assert original.size == (1024, 512)  # the input is not resized in place


def test_image_to_base64_keeps_small_images_at_their_size():
    with Image.open(BytesIO(base64.b64decode(web_gui.image_to_base64(_image((0, 0, 0, 255)))))) as thumb:
        assert thumb.size == (64, 64)


def test_find_free_port_returns_a_bindable_loopback_port():
    port = web_gui.find_free_port()

    server = web_gui.WebPickerServer((web_gui.HOST, port), web_gui.IconPickerHandler, {})
    try:
        assert server.server_address == ("127.0.0.1", port)
    finally:
        server.server_close()


# ---------------------------------------------------------------------------------------------
# the picker server
# ---------------------------------------------------------------------------------------------


def test_picker_binds_only_to_loopback(monkeypatch):
    """Regression: the picker bound ("", port), i.e. every interface, so anyone on the local
    network could open it and pick the app icon."""
    bound = []
    real_init = web_gui.WebPickerServer.__init__

    def spy(self, address, handler, context):
        bound.append(address)
        real_init(self, address, handler, context)

    monkeypatch.setattr(web_gui.WebPickerServer, "__init__", spy)

    _run_picker(monkeypatch, _context(), lambda url: _request(url + "/select", "POST", {"action": "cancel"}))

    assert bound[0][0] == "127.0.0.1"


def test_picker_opens_the_browser_on_the_loopback_url(monkeypatch):
    _, opened, _ = _run_picker(monkeypatch, _context(), lambda url: _request(url + "/select", "POST", {"action": "cancel"}))

    assert len(opened) == 1
    assert opened[0].startswith("http://127.0.0.1:")


def test_get_renders_every_option(monkeypatch):
    context = _context()

    def browse(url):
        page = _request(url)
        _request(url + "/select", "POST", {"action": "cancel"})
        return page

    _, _, (status, html) = _run_picker(monkeypatch, context, browse)

    assert status == 200
    assert "<title>Revenger Icon Picker</title>" in html
    assert "selectImage('sgdb', 0)" in html and "selectImage('sgdb', 1)" in html
    assert "selectImage('igdb', 0)" in html
    assert "selectImage('console', 0)" in html and "selectImage('typo', 0)" in html
    assert web_gui.image_to_base64(context["sgdb"][1]) in html
    assert "512x512" in html  # sizes shown are the originals, not the thumbnails


def test_get_leaves_out_empty_groups(monkeypatch):
    context = {"sgdb": [], "igdb": [], "console": None, "typo": _image((1, 1, 1, 255))}

    def browse(url):
        page = _request(url)
        _request(url + "/select", "POST", {"action": "cancel"})
        return page

    _, _, (_, html) = _run_picker(monkeypatch, context, browse)

    assert "Direct Matches (SGDB)" not in html
    assert "Smart Compositions (IGDB)" not in html
    assert "selectImage('console'" not in html
    assert "selectImage('typo', 0)" in html


@pytest.mark.parametrize("body, expected", [
    ({"group": "sgdb", "index": 1}, ("sgdb", 1)),
    ({"group": "igdb", "index": "0"}, ("igdb", 0)),
    ({"group": "console", "index": 0}, ("console", None)),
    ({"group": "typo", "index": 0}, ("typo", None)),
])
def test_post_selection_returns_that_image(monkeypatch, body, expected):
    context = _context()

    result, _, (status, reply) = _run_picker(monkeypatch, context, lambda url: _request(url + "/select", "POST", body))

    group, index = expected
    assert result is (context[group][index] if index is not None else context[group])
    assert status == 200
    assert json.loads(reply) == {"status": "success"}


@pytest.mark.parametrize("action, expected", [("cancel", "CANCEL"), ("clear_override", "CLEAR_OVERRIDE")])
def test_post_actions_return_their_marker(monkeypatch, action, expected):
    result, _, _ = _run_picker(monkeypatch, _context(), lambda url: _request(url + "/select", "POST", {"action": action}))

    assert result == expected


def test_post_custom_upload_is_decoded_as_rgba(monkeypatch):
    buffer = BytesIO()
    Image.new("RGB", (40, 30), (9, 8, 7)).save(buffer, format="PNG")
    data_url = "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode()

    result, _, _ = _run_picker(
        monkeypatch, _context(), lambda url: _request(url + "/select", "POST", {"custom_base64": data_url}),
    )

    assert result.mode == "RGBA"
    assert result.size == (40, 30)
    assert result.getpixel((0, 0)) == (9, 8, 7, 255)


def test_each_run_starts_without_a_previous_selection(monkeypatch):
    _run_picker(monkeypatch, _context(), lambda url: _request(url + "/select", "POST", {"action": "cancel"}))

    result, _, _ = _run_picker(
        monkeypatch, _context(), lambda url: _request(url + "/select", "POST", {"group": "unknown", "index": 0}),
    )

    assert result is None
