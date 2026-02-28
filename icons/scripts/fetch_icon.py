import os
import re
import requests
from io import BytesIO
from PIL import Image

def load_env():
    env_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), '.env')
    if os.path.exists(env_path):
        with open(env_path, 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#'):
                    if '=' in line:
                        key, val = line.split('=', 1)
                        os.environ[key.strip()] = val.strip().strip("'\"")

load_env()
SGDB_API_KEY = os.environ.get("SGDB_API_KEY")

def clean_rom_name(file_name):
    name_without_tags = re.sub(r'\([^)]*\)|\[[^\]]*\]', '', file_name)
    clean_name = name_without_tags.strip()
    rom_extensions = r'\.(iso|zip|7z|rar|sfc|smc|gba|gbc|gb|nes|n64|z64|v64|rvz|chd|bin|cue|gcm|apk|sms|3ds)$'
    clean_name = re.sub(rom_extensions, '', clean_name, flags=re.IGNORECASE).strip()
    clean_name = clean_name.replace(" - ", ": ")
    return clean_name

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