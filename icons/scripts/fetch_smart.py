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
PLATAFORMAS_IGDB = {
    "mastersystem": 64, "megadrive": 29, "nes": 18, "snes": 19,
    "n64": 4, "gamecube": 21, "gba": 24, "nds": 20, "3ds": 37,
    "ps1": 7, "ps2": 8, "psp": 38
}

def obter_token():
    res = requests.post("https://id.twitch.tv/oauth2/token", params={
        "client_id": CLIENT_ID, "client_secret": CLIENT_SECRET, "grant_type": "client_credentials"
    })
    return res.json().get("access_token") if res.status_code == 200 else None

def buscar_capa_igdb(nome, plat, token):
    p_id = PLATAFORMAS_IGDB.get(plat.lower())
    headers = {"Client-ID": CLIENT_ID, "Authorization": f"Bearer {token}"}
    body = f'search "{nome}"; fields name, cover.url; where platforms = ({p_id}); limit 1;'
    res = requests.post("https://api.igdb.com/v4/games", headers=headers, data=body)
    if res.status_code == 200 and res.json():
        data = res.json()[0]
        if "cover" in data:
            return "https:" + data["cover"]["url"].replace("t_thumb", "t_1080p"), data["name"]
    return None, None

def gerar_icone_smart_image(conteudo):
    img_original = Image.open(BytesIO(conteudo)).convert("RGBA")
    largura, altura = img_original.size
    tamanho_alvo = 512

    proporcao_bg = tamanho_alvo / min(largura, altura)
    bg_w = int(largura * proporcao_bg)
    bg_h = int(altura * proporcao_bg)
    background = img_original.resize((bg_w, bg_h), Image.Resampling.LANCZOS)
    
    left = (bg_w - tamanho_alvo) // 2
    top = (bg_h - tamanho_alvo) // 2
    background = background.crop((left, top, left + tamanho_alvo, top + tamanho_alvo))
    
    background = background.filter(ImageFilter.GaussianBlur(radius=15))
    overlay = Image.new('RGBA', background.size, (0, 0, 0, 77))
    background = Image.alpha_composite(background, overlay)

    proporcao_fg = tamanho_alvo / max(largura, altura)
    fg_w = int(largura * proporcao_fg)
    fg_h = int(altura * proporcao_fg)
    capa_redimensionada = img_original.resize((fg_w, fg_h), Image.Resampling.LANCZOS)

    pos_x = (tamanho_alvo - fg_w) // 2
    pos_y = (tamanho_alvo - fg_h) // 2
    
    background.paste(capa_redimensionada, (pos_x, pos_y), capa_redimensionada)
    return background

def fetch_igdb_smart_icon(platform, rom_name):
    """Fetches the cover from IGDB and generates a smart icon, returning a PIL Image or None."""
    nome_limpo = re.sub(r'\([^)]*\)|\[[^\]]*\]', '', rom_name).strip()
    nome_limpo = re.sub(r'\.(iso|zip|sfc|gba|nds|n64|3ds|bin|cue|sms|nds|gcm)$', '', nome_limpo, flags=re.IGNORECASE).strip()
    
    if platform.lower() not in PLATAFORMAS_IGDB:
        return None
        
    token = obter_token()
    if token:
        url, oficial = buscar_capa_igdb(nome_limpo, platform, token)
        if url:
            try:
                img_data = requests.get(url).content
                return gerar_icone_smart_image(img_data)
            except Exception:
                return None
    return None