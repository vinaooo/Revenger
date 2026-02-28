import os
import sys
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw
from pathlib import Path

# Import the refactored modules
from fetch_icon import fetch_sgdb_icon
from fetch_smart import fetch_igdb_smart_icon
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

def parse_config_xml():
    """Parses config.xml to determine core and rom name."""
    config_path = os.path.join(PROJECT_ROOT, "app", "src", "main", "res", "values", "config.xml")
    try:
        tree = ET.parse(config_path)
        root = tree.getroot()
        core = ""
        rom = ""
        for elem in root.findall("string"):
            if elem.attrib.get("name") == "conf_core":
                core = elem.text or ""
            elif elem.attrib.get("name") == "conf_rom":
                rom = elem.text or ""
        return core.strip(), rom.strip()
    except Exception as e:
        print(f"Error parsing config.xml: {e}")
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

    # Process each density
    for density, size in MIPMAP_SIZES.items():
        folder = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)
        
        # Resize image
        resized = img.resize(size, Image.Resampling.LANCZOS)
        
        # Square/original ic_launcher.png
        resized.save(os.path.join(folder, "ic_launcher.png"), "PNG")
        
        # Round ic_launcher_round.png
        round_img = make_round_image(resized)
        round_img.save(os.path.join(folder, "ic_launcher_round.png"), "PNG")
        
        print(f"✅ Generated {density} icons in {folder}")

import argparse

def main():
    parser = argparse.ArgumentParser(description="Master Icon Creation Orchestrator")
    parser.add_argument("--force", type=int, choices=[1, 2, 3, 4], 
                        help="Force a specific generation method (1=SGDB, 2=IGDB, 3=Console, 4=Typo)")
    parser.add_argument("--skip-downloads", action="store_true", 
                        help="Skip online methods (SGDB, IGDB) and only use local fallbacks")
    
    args = parser.parse_args()
    
    print("🚀 Master Icon Creation Orchestrator")
    
    # Parse config
    core, rom = parse_config_xml()
    if not core or not rom:
        print("❌ Could not determine core or rom from config.xml. Using default values for generation might be needed.")
        sys.exit(1)
        
    platform = determine_platform(core, rom)
    print(f"🎮 Determined Platform: {platform} (Core: {core}, ROM: {rom})")
    
    img = None
    
    if args.force:
        print(f"⚠️ Forcing Method {args.force}")
        if args.force == 1:
            img = fetch_sgdb_icon(rom)
        elif args.force == 2:
            img = fetch_igdb_smart_icon(platform, rom)
        elif args.force == 3:
            img = fetch_console_fallback(platform)
        elif args.force == 4:
            img = generate_typo_icon(rom)
            
    else:
        # Standard cascade
        if not args.skip_downloads:
            # Method 1: SteamGridDB
            print("🔍 Attempting Method 1: SteamGridDB (fetch_icon)...")
            img = fetch_sgdb_icon(rom)
            
            # Method 2: IGDB Smart Icon
            if not img:
                print("🔍 Match not found or failed. Attempting Method 2: IGDB Smart Icon (fetch_smart)...")
                img = fetch_igdb_smart_icon(platform, rom)
        else:
            print("⏭️ Skipping online download methods (SGDB, IGDB)...")
            
        # Method 3: Console Fallback
        if not img:
            print("🔍 Match not found or failed. Attempting Method 3: Console Default Icon...")
            img = fetch_console_fallback(platform)
            
        # Method 4: Typographical Fallback
        if not img:
            print("🔍 Platform icon missing. Attempting Method 4: Typographical Fallback (generate_typo)...")
            img = generate_typo_icon(rom)
            
    if img:
        print("🎉 Image successfully acquired. Generating Android Mipmaps...")
        generate_android_icons(img)
        print("✅ Process complete!")
    else:
        print("❌ All methods failed. Could not generate icon.")

if __name__ == "__main__":
    main()
