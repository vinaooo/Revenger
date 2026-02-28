import os
import requests
from io import BytesIO
from PIL import Image
from utils import load_env, clean_rom_name

load_env()
SGDB_API_KEY = os.environ.get("SGDB_API_KEY")

def search_sgdb_by_text(game_name):
    url = f"https://www.steamgriddb.com/api/v2/search/autocomplete/{game_name}"
    headers = {"Authorization": f"Bearer {SGDB_API_KEY}"}
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        data = response.json().get("data", [])
        if data:
            return data[0]["id"]
    return None

def fetch_steamgriddb_icon(sgdb_id):
    url = f"https://www.steamgriddb.com/api/v2/icons/game/{sgdb_id}?mimes=image/png"
    headers = {"Authorization": f"Bearer {SGDB_API_KEY}"}
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        data = response.json().get("data", [])
        if data:
            return data[0]["url"]
    return None

def fetch_sgdb_icon(rom_name):
    """Fetches the icon from SGDB and returns a PIL Image or None."""
    clean_name = clean_rom_name(rom_name)
    sgdb_id = search_sgdb_by_text(clean_name)
    if not sgdb_id:
        return None
    icon_url = fetch_steamgriddb_icon(sgdb_id)
    if not icon_url:
        return None
    response = requests.get(icon_url)
    if response.status_code == 200:
        try:
            return Image.open(BytesIO(response.content)).convert("RGBA")
        except Exception:
            return None
    return None