"""Stand-ins for `requests`, so the fetcher tests never touch the network."""
from io import BytesIO

import requests
from PIL import Image


def png_bytes(width, height, color=(200, 40, 40, 255)):
    """A solid PNG of the given size, encoded in memory."""
    buffer = BytesIO()
    Image.new("RGBA", (width, height), color).save(buffer, format="PNG")
    return buffer.getvalue()


class FakeResponse:
    def __init__(self, status_code=200, json_data=None, content=b"", json_error=False):
        self.status_code = status_code
        self._json_data = json_data
        self.content = content
        self._json_error = json_error

    def json(self):
        if self._json_error:
            raise ValueError("not JSON")
        return self._json_data


class FakeHttp:
    """Replaces requests.get and requests.post, answering from `routes`.

    `routes` maps a URL prefix to a FakeResponse, an exception instance (raised), or a callable
    taking the call's kwargs. The longest matching prefix wins; an unmatched URL fails the test.
    Every call is recorded in `calls` as (method, url, kwargs), and a call without `timeout=`
    fails the test: a request without a timeout can hang the build forever.
    """

    def __init__(self, monkeypatch, routes=None):
        self.routes = dict(routes or {})
        self.calls = []
        monkeypatch.setattr(requests, "get", self._handler("GET"))
        monkeypatch.setattr(requests, "post", self._handler("POST"))

    def _handler(self, method):
        def handle(url, *args, **kwargs):
            self.calls.append((method, url, kwargs))
            assert "timeout" in kwargs, f"{method} {url} was sent without a timeout"
            matches = [prefix for prefix in self.routes if url.startswith(prefix)]
            assert matches, f"unexpected {method} {url}"
            answer = self.routes[max(matches, key=len)]
            if isinstance(answer, BaseException):
                raise answer
            if callable(answer):
                return answer(kwargs)
            return answer

        return handle

    def urls(self, method=None):
        return [url for m, url, _ in self.calls if method is None or m == method]
