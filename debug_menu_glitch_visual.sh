#!/bin/bash
echo "=== DEBUG MENU FLASHING GLITCH - VISUAL EFFECTS ==="
echo "Capturando logs relacionados ao glitch visual do menu..."
echo "Foco: efeitos visuais de animação, estados das views, timing"
echo "Pressione Ctrl+C para parar"
echo ""
adb logcat | grep -E "(\[ANIMATE_IN\]|\[ANIMATE_OUT\]|\[BATCH_ANIM\]|\[HIDE\]|\[SHOW\]|\[CONFIRM\]|\[DISMISS_MENU\]|\[DISMISS_PUBLIC\]|\[EXECUTE_CONTINUE\]|\[EXECUTE_RESET\]|\[CLEAR_INPUT_STATE\]|\[CLEAR_KEYLOG\]|checkMenuKeyCombo|comboAlreadyTriggered)"

