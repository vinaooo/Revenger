import os
import re
import requests
from PIL import Image, ImageFilter
from io import BytesIO

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
CLIENT_ID = os.environ.get("IGDB_CLIENT_ID")
CLIENT_SECRET = os.environ.get("IGDB_CLIENT_SECRET")
IGDB_PLATFORMS = {
    "mastersystem": 64, "megadrive": 29, "nes": 18, "snes": 19,
    "n64": 4, "gamecube": 21, "gba": 24, "nds": 20, "3ds": 37,
    "ps1": 7, "ps2": 8, "psp": 38
}

def get_token():
    res = requests.post("https://id.twitch.tv/oauth2/token", params={
        "client_id": CLIENT_ID, "client_secret": CLIENT_SECRET, "grant_type": "client_credentials"
    })
    return res.json().get("access_token") if res.status_code == 200 else None

def fetch_igdb_cover(name, platform, token):
    p_id = IGDB_PLATFORMS.get(platform.lower())
    headers = {"Client-ID": CLIENT_ID, "Authorization": f"Bearer {token}"}
    body = f'search "{name}"; fields name, cover.url; where platforms = ({p_id}); limit 1;'
    res = requests.post("https://api.igdb.com/v4/games", headers=headers, data=body)
    if res.status_code == 200 and res.json():
        data = res.json()[0]
        if "cover" in data:
            return "https:" + data["cover"]["url"].replace("t_thumb", "t_1080p"), data["name"]
    return None, None

def generate_smart_icon_image(content):
    img_original = Image.open(BytesIO(content)).convert("RGBA")
    width, height = img_original.size
    target_size = 512

    bg_ratio = target_size / min(width, height)
    bg_w = int(width * bg_ratio)
    bg_h = int(height * bg_ratio)
    background = img_original.resize((bg_w, bg_h), Image.Resampling.LANCZOS)
    
    left = (bg_w - target_size) // 2
    top = (bg_h - target_size) // 2
    background = background.crop((left, top, left + target_size, top + target_size))
    
    background = background.filter(ImageFilter.GaussianBlur(radius=15))
    overlay = Image.new('RGBA', background.size, (0, 0, 0, 77))
    background = Image.alpha_composite(background, overlay)

    fg_ratio = target_size / max(width, height)
    fg_w = int(width * fg_ratio)
    fg_h = int(height * fg_ratio)
    resized_cover = img_original.resize((fg_w, fg_h), Image.Resampling.LANCZOS)

    pos_x = (target_size - fg_w) // 2
    pos_y = (target_size - fg_h) // 2
    
    background.paste(resized_cover, (pos_x, pos_y), resized_cover)
    return background

def fetch_igdb_smart_icon(platform, rom_name):
    """Fetches the cover from IGDB and generates a smart icon, returning a PIL Image or None."""
    clean_name = re.sub(r'\([^)]*\)|\[[^\]]*\]', '', rom_name).strip()
    clean_name = re.sub(r'\.(iso|zip|sfc|gba|nds|n64|3ds|bin|cue|sms|nds|gcm)$', '', clean_name, flags=re.IGNORECASE).strip()
    
    if platform.lower() not in IGDB_PLATFORMS:
        return None
        
    token = get_token()
    if token:
        url, officially_named = fetch_igdb_cover(clean_name, platform, token)
        if url:
            try:
                img_data = requests.get(url).content
                return generate_smart_icon_image(img_data)
            except Exception:
                return None
    return None