import logging
logging.basicConfig(level=logging.INFO, format='%(message)s')
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

def fetch_steamgriddb_icon(sgdb_id, interactive=False):
    url = f"https://www.steamgriddb.com/api/v2/icons/game/{sgdb_id}?mimes=image/png"
    headers = {"Authorization": f"Bearer {SGDB_API_KEY}"}
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        data = response.json().get("data", [])
        if data:
            if interactive:
                print("\n[SteamGridDB] Multiple icons found:")
                limit = min(5, len(data))
                for i in range(limit):
                    print(f"  [{i+1}] {data[i]['url']}")
                while True:
                    choice = input(f"Select an icon (1-{limit}) or 's' to skip SGDB: ").strip().lower()
                    if choice == 's':
                        return None
                    if choice.isdigit():
                        idx = int(choice) - 1
                        if 0 <= idx < limit:
                            return data[idx]["url"]
                    print("Invalid choice, try again.")
            else:
                return data[0]["url"]
    return None

def fetch_sgdb_multiple_icons(rom_name, limit=5):
    """Buscador silencioso que retorna múltiplas opções do SteamGridDB como objetos PIL Image"""
    clean_name = clean_rom_name(rom_name)
    sgdb_id = search_sgdb_by_text(clean_name)
    images = []
    if not sgdb_id:
        return images
        
    url = f"https://www.steamgriddb.com/api/v2/icons/game/{sgdb_id}?mimes=image/png"
    headers = {"Authorization": f"Bearer {SGDB_API_KEY}"}
    response = requests.get(url, headers=headers)
    
    if response.status_code == 200:
        data = response.json().get("data", [])
        for item in data[:limit]:
            try:
                r = requests.get(item["url"], timeout=5)
                if r.status_code == 200:
                    img = Image.open(BytesIO(r.content)).convert("RGBA")
                    images.append(img)
            except Exception as e:
                logging.debug(f"Erro ao baixar {item['url']}: {e}")
    return images

def fetch_sgdb_icon(rom_name, interactive=False):
    """Fetches the icon from SGDB and returns a PIL Image or None."""
    clean_name = clean_rom_name(rom_name)
    sgdb_id = search_sgdb_by_text(clean_name)
    if not sgdb_id:
        return None
    icon_url = fetch_steamgriddb_icon(sgdb_id, interactive)
    if not icon_url:
        return None
    response = requests.get(icon_url)
    if response.status_code == 200:
        try:
            return Image.open(BytesIO(response.content)).convert("RGBA")
        except Exception:
            return None
    return None