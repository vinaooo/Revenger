import os
import re

def load_env():
    """Loads the .env file located one directory up from this script."""
    env_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), '.env')
    if os.path.exists(env_path):
        with open(env_path, 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#'):
                    if '=' in line:
                        key, val = line.split('=', 1)
                        os.environ[key.strip()] = val.strip().strip("'\"")

def clean_rom_name(file_name):
    """Removes platform extensions and metadata tags from a ROM filename."""
    name_without_tags = re.sub(r'\([^)]*\)|\[[^\]]*\]', '', file_name)
    clean_name = name_without_tags.strip()
    rom_extensions = r'\.(iso|zip|7z|rar|sfc|smc|gba|gbc|gb|nes|n64|z64|v64|rvz|chd|bin|cue|gcm|apk|sms|3ds|md|gen)$'
    clean_name = re.sub(rom_extensions, '', clean_name, flags=re.IGNORECASE).strip()
    clean_name = clean_name.replace(" - ", ": ")
    return clean_name
