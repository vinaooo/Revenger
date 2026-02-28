import os
import re
import hashlib
from PIL import Image, ImageDraw, ImageFont

OUTPUT_SIZE = 512

COLOR_PALETTE = [
    (0x3F, 0x51, 0xB5), (0xE9, 0x1E, 0x63), (0x21, 0x96, 0xF3),
    (0x4C, 0xAF, 0x50), (0xFF, 0x57, 0x22), (0xFF, 0x98, 0x00),
    (0x9C, 0x27, 0xB0), (0x00, 0x96, 0x88), (0xFF, 0xEB, 0x3B),
    (0x03, 0xA9, 0xF4)
]

FONT_PATH = "/usr/share/fonts/truetype/ubuntu/Ubuntu-B.ttf"
if not os.path.exists(FONT_PATH):
    FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

def clean_rom_name(file_name):
    name_without_tags = re.sub(r'\([^)]*\)|\[[^\]]*\]', '', file_name)
    clean_name = name_without_tags.strip()
    rom_extensions = r'\.(iso|zip|sfc|gba|nds|n64|3ds|bin|cue|sms|nds|gcm)$'
    clean_name = re.sub(rom_extensions, '', clean_name, flags=re.IGNORECASE).strip()
    return clean_name

def generate_stable_color(game_name):
    hash_obj = hashlib.md5(game_name.encode('utf-8'))
    hash_hex = hash_obj.hexdigest()
    color_index = int(hash_hex[:2], 16) % len(COLOR_PALETTE)
    return COLOR_PALETTE[color_index]

def extract_initials(game_name):
    words = re.findall(r'\b\w+\b', game_name)
    if not words: return "?"
    initials = [w[0].upper() for w in words[:3]]
    return "".join(initials)

def generate_typo_icon(rom_name):
    """Generates a typographical material design icon, returning a PIL Image."""
    clean_name = clean_rom_name(rom_name)
    initials = extract_initials(clean_name)
    bg_color = generate_stable_color(clean_name)
    
    icon = Image.new("RGBA", (OUTPUT_SIZE, OUTPUT_SIZE), (bg_color[0], bg_color[1], bg_color[2], 255))
    draw = ImageDraw.Draw(icon)
    
    text_color = (255, 255, 255, 255)
    if bg_color == COLOR_PALETTE[8]:
        text_color = (33, 33, 33, 255)
    
    try:
        font_size = int(OUTPUT_SIZE * 0.6)
        if len(initials) > 2: font_size = int(OUTPUT_SIZE * 0.45)
        font = ImageFont.truetype(FONT_PATH, font_size)
    except Exception:
        font = ImageFont.load_default()

    text_bbox = draw.textbbox((0, 0), initials, font=font)
    text_width = text_bbox[2] - text_bbox[0]
    text_height = text_bbox[3] - text_bbox[1]
    
    pos_x = (OUTPUT_SIZE - text_width) // 2
    pos_y = ((OUTPUT_SIZE - text_height) // 2) - text_bbox[1]
    
    draw.text((pos_x, pos_y), initials, fill=text_color, font=font)
    return icon