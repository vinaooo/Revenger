# Config JSON structure
- `default_settings`: (Boolean) When true, gameplay config is loaded from `default_settings.json`. When false, it's loaded from `config_manual.json`.
- `platform`: (String) Platform identifier (e.g., sms, snes, gba, gb, md, nes, gbc). Used as fallback.
- `core`: (String) LibRetro core (excluding `_libretro_android.so`).
- `rom`: (String) ROM filename (placed in `roms_backup/` and staged automatically).
- `target_abi`: (String) Target ABI: x86, x86_64, armeabi-v7a, arm64-v8a, or all.
- `load_bytes`: (Boolean) Load ROM directly into memory; disable for larger games.

Changes to how these keys are parsed or merged (`AppConfig`, `BaseConfig`) need test coverage — see `tests/AppConfig_test.kt` and the testing policy in `CLAUDE.md` / `definitions/Code.md`.
