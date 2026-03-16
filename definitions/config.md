# Config JSON structure
- `conf_default_settings`: (Boolean) When true, gameplay config is loaded from `default_settings.json`. When false, it's loaded from `config_manual.json`.
- `conf_platform`: (String) Platform identifier (e.g., sms, snes, gba, gb, md, nes, gbc). Used as fallback.
- `conf_core`: (String) LibRetro core (excluding `_libretro_android.so`).
- `conf_rom`: (String) ROM filename (placed in `roms_backup/` and staged automatically).
- `conf_target_abi`: (String) Target ABI: x86, x86_64, armeabi-v7a, arm64-v8a, or all.
- `conf_load_bytes`: (Boolean) Load ROM directly into memory; disable for larger games.
