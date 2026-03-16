# Config Manual JSON structure
Settings used when `conf_default_settings` is false.
- `conf_variables`: (String) Core variables passed verbatim to the LibRetro core.
- `conf_fast_forward_multiplier`: (Integer) Fast forward speed multiplier.
- `conf_fullscreen`: (Boolean) Fullscreen mode.
- `conf_orientation`: (Integer) 1 = Portrait only, 2 = Landscape only, 3 = Auto.

## Menu Modes
- `conf_menu_mode_fab`: (String) Floating action button position.
- `conf_menu_mode_gamepad`: (Boolean) Open menu via START button.
- `conf_menu_mode_back`: (Boolean) Open menu via Android back button.
- `conf_menu_mode_combo`: (Boolean) Open menu via SELECT + START combo.

## Virtual Gamepad
- `conf_gamepad`: (Boolean) Show virtual gamepad.
- `conf_gp_haptic`: (Boolean) Vibrate on touch.
- `conf_gp_allow_multiple_presses_action`: (Boolean) Allow multiple simultaneous presses.
- Actions: `conf_gp_a`, `conf_gp_b`, `conf_gp_x`, `conf_gp_y` - (Boolean).
- System: `conf_gp_start`, `conf_gp_select` - (Boolean).
- Shoulders: `conf_gp_l1`, `conf_gp_r1`, `conf_gp_l2`, `conf_gp_r2` - (Boolean).
- `conf_left_analog`: (Boolean) Replace left D-pad with analog stick.

## Fake Buttons
- `conf_show_fake_button_X`: (Boolean) Maps to in-game functions.

## Video & Debug
- `conf_shader`: (String) Shader type.
- `conf_performance_overlay`: (Boolean) FPS overlay.
