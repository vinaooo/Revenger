import os
import re
import requests
from io import BytesIO
from PIL import Image

SGDB_API_KEY = "de80a1445d03aae6e3227c2aaf141a4e"

def limpar_nome_rom(nome_arquivo):
    nome_sem_tags = re.sub(r'\([^)]*\)|\[[^\]]*\]', '', nome_arquivo)
    nome_limpo = nome_sem_tags.strip()
    extensoes_rom = r'\.(iso|zip|7z|rar|sfc|smc|gba|gbc|gb|nes|n64|z64|v64|rvz|chd|bin|cue|gcm|apk|sms|3ds)$'
    nome_limpo = re.sub(extensoes_rom, '', nome_limpo, flags=re.IGNORECASE).strip()
    nome_limpo = nome_limpo.replace(" - ", ": ")
    return nome_limpo

def buscar_sgdb_por_texto(nome_jogo):
    url = f"https://www.steamgriddb.com/api/v2/search/autocomplete/{nome_jogo}"
    headers = {"Authorization": f"Bearer {SGDB_API_KEY}"}
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        data = response.json().get("data", [])
        if data:
            return data[0]["id"]
    return None

def buscar_icone_steamgriddb(sgdb_id):
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
    nome_limpo = limpar_nome_rom(rom_name)
    sgdb_id = buscar_sgdb_por_texto(nome_limpo)
    if not sgdb_id:
        return None
    icon_url = buscar_icone_steamgriddb(sgdb_id)
    if not icon_url:
        return None
    response = requests.get(icon_url)
    if response.status_code == 200:
        try:
            return Image.open(BytesIO(response.content)).convert("RGBA")
        except Exception:
            return None
    return None