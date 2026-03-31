import logging
logging.basicConfig(level=logging.INFO, format='%(message)s')
import os
import sys
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw
from pathlib import Path

# Import the refactored modules
from fetch_icon import fetch_sgdb_icon, fetch_sgdb_multiple_icons
from fetch_smart import fetch_igdb_smart_icon, fetch_igdb_multiple_covers
from generate_typo import generate_typo_icon

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# The script is in icons/scripts now, so root is two levels up
PROJECT_ROOT = os.path.dirname(os.path.dirname(SCRIPT_DIR))

# Console icons mapping
CONSOLE_ICONS = {
    "mastersystem": "sega_master_system_mark_iii_mk_3006.png",
    "megadrive": "sega_mega_cd_sega_cd.png",
    "nes": "nintendo_nintendo_entertainment_system_hvc_001.png",
    "snes": "nintendo_super_nintendo_entertainment_system_sns_001.png",
    "n64": "nintendo_nintendo_64dd.png",
    "gba": "nintendo_game_boy_advance_agb_001_b.png",
    "nds": "nintendo_nintendo_ds_ntr_001_b.png",
    "3ds": "nintendo_nintendo_3ds_ctr_001_a.png",
    "ps1": "sony_playstation.png",
    "ps2": "sony_playstation_2_scph_90000.png",
    "psp": "sony_playstation_portable.png",
    "gb": "nintendo_game_boy.png"
}

# Core to platform fallback mapping
CORE_TO_PLATFORM = {
    "citra": "3ds",
    "snes9x": "snes",
    "genesis_plus_gx": "megadrive",
    "picodrive": "megadrive",
    "gambatte": "gb",
    "gearsystem": "mastersystem",
    "mupen64plus": "n64",
    "mgba": "gba",
    "melonds": "nds",
    "pcsx_rearmed": "ps1",
    "duckstation": "ps1",
    "play": "ps2",
    "pcsx2": "ps2",
    "ppsspp": "psp",
    "dolphin": "gamecube"
}

# Android mipmap sizes
MIPMAP_SIZES = {
    "mdpi": (48, 48),
    "hdpi": (72, 72),
    "xhdpi": (96, 96),
    "xxhdpi": (144, 144),
    "xxxhdpi": (192, 192)
}

import json
def parse_config_xml(config_file_path=None):
    """Parses config.json to determine core and rom name."""
    if not config_file_path:
        config_file_path = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "config", "config.json")
        
    try:
        with open(config_file_path, 'r', encoding='utf-8') as f:
            config_data = json.load(f)
            
        manual_file_path = os.path.join(os.path.dirname(config_file_path), "config_manual.json")
        if os.path.exists(manual_file_path):
            with open(manual_file_path, 'r', encoding='utf-8') as f:
                manual_data = json.load(f)
                config_data.update(manual_data)
        
        core_value = config_data.get('core', "")
        
        if config_data.get('default_settings', False):
            # Try to grab core from optimal_settings
            platform_id = config_data.get('platform', "")
            rom_value = config_data.get('rom', "")
            ext = "." + rom_value.split('.')[-1].lower() if '.' in rom_value else ''
            
            default_file_path = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "default_settings.json")
            if os.path.exists(default_file_path):
                with open(default_file_path, 'r', encoding='utf-8') as f:
                    default_data = json.load(f)
                    
                profile = None
                if platform_id:
                    profile = next((p for p in default_data if p.get('platform_id') == platform_id), None)
                if not profile:
                    profile = next((p for p in default_data if ext in p.get('extensions', [])), None)
                    
                if profile and profile.get('core'):
                    core_value = profile.get('core')
        
        rom_value = config_data.get('rom', "")
        return core_value.strip(), rom_value.strip()
    except Exception as e:
        logging.error(f"Error parsing config.json at {config_file_path}: {e}")
        return "", ""


def determine_platform(core, rom):
    """Determines the platform from the ROM extension or libretro core."""
    ext = rom.split('.')[-1].lower() if '.' in rom else ''
    
    # Check extension first
    ext_map = {
        "3ds": "3ds",
        "sfc": "snes", "smc": "snes",
        "md": "megadrive", "bin": "megadrive", "gen": "megadrive",
        "sms": "mastersystem",
        "nes": "nes",
        "n64": "n64", "z64": "n64",
        "gba": "gba",
        "nds": "nds"
    }
    
    if ext in ext_map:
        # Avoid conflicts (e.g., .bin could be megadrive or ps1)
        if ext == "bin":
            if "pcsx" in core or "duck" in core: return "ps1"
            return "megadrive"
        return ext_map[ext]
        
    # Fallback to core
    core_clean = core.replace("_libretro_android", "").replace("_libretro", "")
    for key, plat in CORE_TO_PLATFORM.items():
        if key in core_clean:
            return plat
            
    return "unknown"

def fetch_console_fallback(platform):
    """Returns the preset console image."""
    icon_name = CONSOLE_ICONS.get(platform)
    if not icon_name:
        return None
        
    # The console images are now stored in an 'images' directory alongside 'scripts'
    icons_dir = os.path.dirname(SCRIPT_DIR)
    icon_path = os.path.join(icons_dir, "images", icon_name)
    if os.path.exists(icon_path):
        try:
            return Image.open(icon_path).convert("RGBA")
        except Exception:
            return None
    return None

def make_perfect_square(img):
    """Pads image into a perfect square by mirroring/blurring edges without distortion."""
    w, h = img.size
    if w == h:
        return img
    
    size = max(w, h)
    
    # Resize heavily to fill canvas and apply heavy blur for a seamless background
    from PIL import ImageFilter
    bg = img.resize((size, size), Image.Resampling.LANCZOS)
    bg = bg.filter(ImageFilter.GaussianBlur(radius=15))
    
    paste_x = (size - w) // 2
    paste_y = (size - h) // 2
    
    final_img = Image.new("RGBA", (size, size))
    final_img.paste(bg, (0, 0))
    final_img.paste(img, (paste_x, paste_y), img if img.mode == 'RGBA' else None)
    
    return final_img

def make_squircle_image(img, radius=0.15):
    """Masks a square image into a squircle (rounded corners) for legacy squares."""
    size = img.size
    mask = Image.new('L', size, 0)
    draw = ImageDraw.Draw(mask)
    rad = int(size[0] * radius)
    draw.rounded_rectangle((0, 0) + size, radius=rad, fill=255)
    
    round_img = Image.new('RGBA', size)
    round_img.paste(img, (0, 0), mask=mask)
    return round_img

def make_round_image(img):
    """Masks an Image into a circle."""
    size = img.size
    mask = Image.new('L', size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0) + size, fill=255)
    
    round_img = Image.new('RGBA', size)
    round_img.paste(img, (0, 0), mask=mask)
    return round_img

def generate_android_icons(img):
    """Generates and saves mipmap icons from the base image."""
    res_dir = os.path.join(PROJECT_ROOT, "app", "src", "main", "res")

    # Assegura que todas as imagens enviadas ao Android são rigorosamente Quadrados Perfeitos e Preenchidos
    img = make_perfect_square(img)
    img_w, img_h = img.size

    # Process each density
    for density, size in MIPMAP_SIZES.items():
        folder = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)
        
        # Como o img já é um quadrado perfeito, escalar não precisa mais da matemática complexa de crop:
        target_w, target_h = size
        scaled_img = img.resize((target_w, target_h), Image.Resampling.LANCZOS)
        
        # 1. Square Legacy (agora com um suave "Squircle / Rounded Corners" de margem 15% como pedido)
        square_img = make_squircle_image(scaled_img, radius=0.15)
        square_img.save(os.path.join(folder, "ic_launcher.png"), "PNG")
        
        # 2. Round Legacy (Círculo de bordas perfeitas - 100% de raio sobre a imagem padronizada)
        round_img = make_round_image(scaled_img)
        round_img.save(os.path.join(folder, "ic_launcher_round.png"), "PNG")
        
        # Adaptive Icon Foregrounds (108dp base, size multiplier 108/48 = 2.25)
        # However, to prevent zooming/clipping by OEM mask systems, the actual 
        # logo needs to fit within the Safe Zone (~66% of the 108dp).
        adaptive_target_w = int(target_w * 2.25)
        adaptive_target_h = int(target_h * 2.25)
        
        # 1. Safe zone width (approx 66-70% of total adaptive width)
        safe_zone_w = int(adaptive_target_w * 0.68)
        safe_zone_h = int(adaptive_target_h * 0.68)
        
        # 2. Scale image precisely to fit inside Safe Zone
        ratio_safe = min(safe_zone_w / img_w, safe_zone_h / img_h)
        new_w_safe = int(img_w * ratio_safe)
        new_h_safe = int(img_h * ratio_safe)
        scaled_img_safe = img.resize((new_w_safe, new_h_safe), Image.Resampling.LANCZOS)
        
        # 3. Create full blank 108dp (Adaptive) canvas
        canvas_foreground = Image.new("RGBA", (adaptive_target_w, adaptive_target_h), (0, 0, 0, 0))
        canvas_background = Image.new("RGBA", (adaptive_target_w, adaptive_target_h), (30, 30, 30, 255))
        
        # 4. Paste safe image exactly in the center of the adaptive canvas
        paste_x = (adaptive_target_w - new_w_safe) // 2
        paste_y = (adaptive_target_h - new_h_safe) // 2
        
        # O foreground terá o logo centralizado respeitando o Safe Zone
        canvas_foreground.paste(scaled_img_safe, (paste_x, paste_y), scaled_img_safe)
        # O background pode ser também o logo com muito blur esticado até as bordas pra ficar bonito,
        # ou apenas a cor sólida (no caso, estamos esticando do jeito antigo só para fundo ou usando cor).
        # Para ser estéticamente agradável e simular o blur parallax
        ratio_bg = max(adaptive_target_w / img_w, adaptive_target_h / img_h)
        bg_w_ad = int(img_w * ratio_bg)
        bg_h_ad = int(img_h * ratio_bg)
        scaled_bg = img.resize((bg_w_ad, bg_h_ad), Image.Resampling.LANCZOS)
        l_ad = (bg_w_ad - adaptive_target_w) // 2
        t_ad = (bg_h_ad - adaptive_target_h) // 2
        cropped_bg = scaled_bg.crop((l_ad, t_ad, l_ad + adaptive_target_w, t_ad + adaptive_target_h))
        # Aplica desfoque profundo para servir de fundo parallax se a logo não preencher bordas
        from PIL import ImageFilter
        frozen_bg = cropped_bg.filter(ImageFilter.GaussianBlur(radius=8))
        canvas_background.paste(frozen_bg, (0,0), frozen_bg)
        
        # Save foreground and background
        canvas_foreground.save(os.path.join(folder, "ic_launcher_foreground.png"), "PNG")
        canvas_background.save(os.path.join(folder, "ic_launcher_background.png"), "PNG")
        
        logging.info(f"✅ Generated {density} icons in {folder}")

    # Generate adaptive icon XMLs in mipmap-anydpi-v26
    anydpi_folder = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(anydpi_folder, exist_ok=True)
    
    adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>'''
    
    with open(os.path.join(anydpi_folder, "ic_launcher.xml"), "w") as f:
        f.write(adaptive_xml)
    with open(os.path.join(anydpi_folder, "ic_launcher_round.xml"), "w") as f:
        f.write(adaptive_xml)
        
    logging.info(f"✅ Generated adaptive icon XMLs in {anydpi_folder}")

import argparse

def main():
    parser = argparse.ArgumentParser(description="Master Icon Creation Orchestrator")
    parser.add_argument("--force", type=int, choices=[1, 2, 3, 4], 
                        help="Force a specific generation method (1=SGDB, 2=IGDB, 3=Console, 4=Typo)")
    parser.add_argument("--skip-downloads", action="store_true", 
                        help="Skip online methods (SGDB, IGDB) and only use local fallbacks")
    parser.add_argument("--interactive", "-i", action="store_true", 
                        help="Prompt the user to choose between multiple search results")
    parser.add_argument("--gui-web", action="store_true", 
                        help="Start a local web server to pick the icon from ALL generators entirely graphically.")
    parser.add_argument("--config", type=str, 
                        help="Optional absolute path to config.xml. If omitted, assumes default Revenger project structure.")
    
    args = parser.parse_args()
    
    logging.info("🚀 Master Icon Creation Orchestrator")
    
    # Parse config
    core, rom = parse_config_xml(args.config)
    if not core or not rom:
        logging.error("❌ Could not determine core or rom from config.xml. Using default values for generation might be needed.")
        sys.exit(1)
        
    platform = determine_platform(core, rom)
    logging.info(f"🎮 Determined Platform: {platform} (Core: {core}, ROM: {rom})")
    
    img = None
    
    # -----------------------------------------------------------------
    # CACHE/OVERRIDE CHECK
    # Se NÃO estamos na gui-web, verificar se o dev escolheu uma override para ESSA rom
    # -----------------------------------------------------------------
    if not args.gui_web:
        override_dir = os.path.join(PROJECT_ROOT, "icons", ".cache")
        override_path = os.path.join(override_dir, "custom_override_icon.png")
        rom_lock_path = os.path.join(override_dir, "last_rom.txt")
        
        if os.path.exists(override_path) and os.path.exists(rom_lock_path):
            with open(rom_lock_path, 'r', encoding='utf-8') as f:
                locked_rom = f.read().strip()
                
            if locked_rom == rom:
                logging.info(f"🔒 Found manual Web Interface override icon for {rom}. Skipping auto-scraping!")
                img = Image.open(override_path).convert("RGBA")
                
                # Vai direto para o gerador de mipmap e mata a branch atual
                generate_android_icons(img)
                logging.info("✅ Process complete (Using Cached Override)!")
                return
            else:
                logging.info("♻️ Locked ROM is different from current ROM. Dropping obsolete icon cache.")
                try: 
                    os.remove(override_path)
                    os.remove(rom_lock_path)
                except: 
                    pass

    if args.gui_web:
        logging.info("🚀 [WEB GUI] Launching interactive showcase... Fetching all possible variations.")
        from web_gui import start_web_picker
        
        # Coletar em paralelo/rápido de todos os geradores
        print("  -> Searching SteamGridDB...")
        sgdb_imgs = fetch_sgdb_multiple_icons(rom, limit=5)
        print(f"  -> Searching IGDB for {platform}...")
        igdb_imgs = fetch_igdb_multiple_covers(platform, rom, limit=5)
        print("  -> Generating Local Fallbacks...")
        console_img = fetch_console_fallback(platform)
        typo_img = generate_typo_icon(rom)
        
        # Padding prévio para a Web GUI mostrar o quadrado perfeito + blur nas prévias transparentes
        if console_img: console_img = make_perfect_square(console_img)
        if typo_img: typo_img = make_perfect_square(typo_img)
        
        ctx = {
            "sgdb": [make_perfect_square(i) for i in sgdb_imgs] if sgdb_imgs else [],
            "igdb": [make_perfect_square(i) for i in igdb_imgs] if igdb_imgs else [],
            "console": console_img,
            "typo": typo_img
        }
        
        img = start_web_picker(ctx)
        
        if img == "CANCEL":
            logging.info("⏹️ User cancelled the operation via Web GUI. Exiting gracefully without further modifications.")
            sys.exit(0)

        if img == "CLEAR_OVERRIDE":
            logging.info("🗑️ Restore Auto-Scraping requested! Clearing manual override lock...")
            override_dir = os.path.join(PROJECT_ROOT, "icons", ".cache")
            override_path = os.path.join(override_dir, "custom_override_icon.png")
            rom_lock_path = os.path.join(override_dir, "last_rom.txt")
            if os.path.exists(override_path): os.remove(override_path)
            if os.path.exists(rom_lock_path): os.remove(rom_lock_path)
            
            # Switch modes so the standard cascade runs immediately below
            args.gui_web = False
            img = None

    if not args.gui_web and not img:
        if args.force:
            logging.warning(f"⚠️ Forcing Method {args.force}")
            if args.force == 1:
                img = fetch_sgdb_icon(rom, interactive=args.interactive)
            elif args.force == 2:
                img = fetch_igdb_smart_icon(platform, rom, interactive=args.interactive)
            elif args.force == 3:
                img = fetch_console_fallback(platform)
            elif args.force == 4:
                img = generate_typo_icon(rom)
                
        else:
            # Standard cascade
            if not args.skip_downloads:
                # Method 1: SteamGridDB
                logging.info("🔍 Attempting Method 1: SteamGridDB (fetch_icon)...")
                img = fetch_sgdb_icon(rom, interactive=args.interactive)
                
                # Method 2: IGDB Smart Icon
                if not img:
                    logging.info("🔍 Match not found or failed. Attempting Method 2: IGDB Smart Icon (fetch_smart)...")
                    img = fetch_igdb_smart_icon(platform, rom, interactive=args.interactive)
            else:
                logging.info("⏭️ Skipping online download methods (SGDB, IGDB)...")
                
            # Method 3: Console Fallback
            if not img:
                logging.info("🔍 Match not found or failed. Attempting Method 3: Console Default Icon...")
                img = fetch_console_fallback(platform)
                
            # Method 4: Typographical Fallback
            if not img:
                logging.info("🔍 Platform icon missing. Attempting Method 4: Typographical Fallback (generate_typo)...")
                img = generate_typo_icon(rom)
            
    if img:        # Quando rodando via --gui-web, antes de fechar salva o lock (cópia crua na pasta icons)
        if args.gui_web:
            override_dir = os.path.join(PROJECT_ROOT, "icons", ".cache")
            os.makedirs(override_dir, exist_ok=True)
            override_path = os.path.join(override_dir, "custom_override_icon.png")
            rom_lock_path = os.path.join(override_dir, "last_rom.txt")
            
            img.save(override_path, "PNG")
            with open(rom_lock_path, 'w', encoding='utf-8') as f:
                f.write(rom)
            logging.info(f"🔒 Custom interactively selected icon saved as override lock for ROM: {rom}")
        logging.info("🎉 Image successfully acquired. Generating Android Mipmaps...")
        generate_android_icons(img)
        logging.info("✅ Process complete!")
    else:
        logging.error("❌ All methods failed. Could not generate icon.")

if __name__ == "__main__":
    main()
