# Config Manual JSON structure
Settings used when `default_settings` is false.
- `variables`: (String) Core variables passed verbatim to the LibRetro core.
- `fast_forward_multiplier`: (Integer) Fast forward speed multiplier.
- `fullscreen`: (Boolean) Fullscreen mode.
- `orientation`: (String) "portrait" = Portrait only, "landscape" = Landscape only, "auto" = Any orientation (respects system auto-rotate).

## Menu Modes
- `menu_mode`: (String) Comma-separated list defining how the menu can be opened. The order of words does not matter.
  - Can include any combination of: `gamepad`, `back`, `combo`. 
  - To configure the floating action button (FAB) position, append `fab=` followed by one of the 4 quadrants: `fab=bottom-right`, `fab=bottom-left`, `fab=top-right`, or `fab=top-left`.
  - To hide the FAB entirely, simply omit `fab=` from the string.
  - Example: `gamepad,back,combo,fab=bottom-right`

## Virtual Gamepad
- `gamepad`: (Boolean) Show virtual gamepad.
- `gp_haptic`: (Boolean) Vibrate on touch.
- `button_allow_multiple_presses_action`: (Boolean) Allow multiple simultaneous presses.
- Actions: `button_a`, `button_b`, `button_x`, `button_y` - (Boolean).
- System: `button_start`, `button_select` - (Boolean).
- Shoulders: `button_l1`, `button_r1`, `button_l2`, `button_r2` - (Boolean).
- `left_analog`: (Boolean) Replace left D-pad with analog stick.

## Fake Buttons
- `fake_button_X`: (Boolean) Maps to in-game functions.

## Video & Debug
- `shader`: (String) Shader type.
- `performance_overlay`: (Boolean) FPS overlay.
