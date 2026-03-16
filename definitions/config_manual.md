# Config Manual JSON structure
Settings used when `default_settings` is false.
- `variables`: (String) Core variables passed verbatim to the LibRetro core.
- `fast_forward_multiplier`: (Integer) Fast forward speed multiplier.
- `fullscreen`: (Boolean) Fullscreen mode.
- `orientation`: (Integer) 1 = Portrait only, 2 = Landscape only, 3 = Auto.

## Menu Modes
- `menu_mode_fab`: (String) Floating action button position.
- `menu_mode_gamepad`: (Boolean) Open menu via START button.
- `menu_mode_back`: (Boolean) Open menu via Android back button.
- `menu_mode_combo`: (Boolean) Open menu via SELECT + START combo.

## Virtual Gamepad
- `gamepad`: (Boolean) Show virtual gamepad.
- `gp_haptic`: (Boolean) Vibrate on touch.
- `gp_allow_multiple_presses_action`: (Boolean) Allow multiple simultaneous presses.
- Actions: `gp_a`, `gp_b`, `gp_x`, `gp_y` - (Boolean).
- System: `gp_start`, `gp_select` - (Boolean).
- Shoulders: `gp_l1`, `gp_r1`, `gp_l2`, `gp_r2` - (Boolean).
- `left_analog`: (Boolean) Replace left D-pad with analog stick.

## Fake Buttons
- `show_fake_button_X`: (Boolean) Maps to in-game functions.

## Video & Debug
- `shader`: (String) Shader type.
- `performance_overlay`: (Boolean) FPS overlay.
