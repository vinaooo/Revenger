#!/bin/bash
echo "=== LOGCAT DEBUG MENU GLITCH ==="
echo "Capturando logs relacionados ao glitch do menu..."
echo "Pressione Ctrl+C para parar"
echo ""
adb logcat | grep -E "(\[CONFIRM\]|\[DISMISS_MENU\]|\[EXECUTE_CONTINUE\]|\[EXECUTE_RESET\]|\[CLEAR_INPUT_STATE\]|\[CLEAR_KEYLOG\]|checkMenuKeyCombo|comboAlreadyTriggered)"

