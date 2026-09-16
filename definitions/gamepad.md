# Gamepad JSON structure
- `button_button_color`: (String) Gamepad button colors for virtual controller (HEX color format).
- `gp_pressed_color`: (String) Pressed button color (HEX color format).
- `gp_padding_vertical`: (String) Vertical padding (e.g. `20dp`).
- `gp_offset_portrait`: (Integer) Vertical offset (0-100%).
- `gp_offset_landscape`: (Integer) Horizontal offset (0-100%).

Changes to how these keys are parsed or used need test coverage — see `tests/GamePadConfig_test.kt` and `tests/GamePadAlignmentManager_test.kt`, and the testing policy in `CLAUDE.md` / `definitions/Code.md`.
