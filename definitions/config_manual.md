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
- `button_allow_multiple_presses_action`: (Boolean) Allow multiple simultaneous presses.
- Actions: `button_a`, `button_b`, `button_x`, `button_y` - (Boolean).
- System: `button_start`, `button_select` - (Boolean).
- Shoulders: `button_l1`, `button_r1`, `button_l2`, `button_r2` - (Boolean).
- `left_analog`: (Boolean) Replace left D-pad with analog stick.

## Fake Buttons
- `show_fake_button_X`: (Boolean) Maps to in-game functions.

## Video & Debug
- `shader`: (String) Shader type.
- `performance_overlay`: (Boolean) FPS overlay.
