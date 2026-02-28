import os
import re
import hashlib
from PIL import Image, ImageDraw, ImageFont

OUTPUT_SIZE = 512

CORES_PALETA = [
    (0x3F, 0x51, 0xB5), (0xE9, 0x1E, 0x63), (0x21, 0x96, 0xF3),
    (0x4C, 0xAF, 0x50), (0xFF, 0x57, 0x22), (0xFF, 0x98, 0x00),
    (0x9C, 0x27, 0xB0), (0x00, 0x96, 0x88), (0xFF, 0xEB, 0x3B),
    (0x03, 0xA9, 0xF4)
]

FONT_PATH = "/usr/share/fonts/truetype/ubuntu/Ubuntu-B.ttf"
if not os.path.exists(FONT_PATH):
    FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

def limpar_nome_rom(nome_arquivo):
    nome_sem_tags = re.sub(r'\([^)]*\)|\[[^\]]*\]', '', nome_arquivo)
    nome_limpo = nome_sem_tags.strip()
    extensoes_rom = r'\.(iso|zip|sfc|gba|nds|n64|3ds|bin|cue|sms|nds|gcm)$'
    nome_limpo = re.sub(extensoes_rom, '', nome_limpo, flags=re.IGNORECASE).strip()
    return nome_limpo

def gerar_cor_estavel(nome_jogo):
    hash_obj = hashlib.md5(nome_jogo.encode('utf-8'))
    hash_hex = hash_obj.hexdigest()
    indice_cor = int(hash_hex[:2], 16) % len(CORES_PALETA)
    return CORES_PALETA[indice_cor]

def extrair_iniciais(nome_jogo):
    palavras = re.findall(r'\b\w+\b', nome_jogo)
    if not palavras: return "?"
    iniciais = [p[0].upper() for p in palavras[:3]]
    return "".join(iniciais)

def generate_typo_icon(rom_name):
    """Generates a typographical material design icon, returning a PIL Image."""
    nome_limpo = limpar_nome_rom(rom_name)
    iniciais = extrair_iniciais(nome_limpo)
    cor_fundo = gerar_cor_estavel(nome_limpo)
    
    icone = Image.new("RGBA", (OUTPUT_SIZE, OUTPUT_SIZE), (cor_fundo[0], cor_fundo[1], cor_fundo[2], 255))
    draw = ImageDraw.Draw(icone)
    
    cor_texto = (255, 255, 255, 255)
    if cor_fundo == CORES_PALETA[8]:
        cor_texto = (33, 33, 33, 255)
    
    try:
        font_size = int(OUTPUT_SIZE * 0.6)
        if len(iniciais) > 2: font_size = int(OUTPUT_SIZE * 0.45)
        font = ImageFont.truetype(FONT_PATH, font_size)
    except Exception:
        font = ImageFont.load_default()

    text_bbox = draw.textbbox((0, 0), iniciais, font=font)
    text_width = text_bbox[2] - text_bbox[0]
    text_height = text_bbox[3] - text_bbox[1]
    
    pos_x = (OUTPUT_SIZE - text_width) // 2
    pos_y = ((OUTPUT_SIZE - text_height) // 2) - text_bbox[1]
    
    draw.text((pos_x, pos_y), iniciais, fill=cor_texto, font=font)
    return icone