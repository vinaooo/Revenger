#!/bin/bash
echo "Capturando logs de navegação do menu RetroMenu3..."
adb logcat -c
echo "Logs limpos. Agora abra o app e navegue pelo menu usando D-PAD."
echo "Pressione Ctrl+C quando terminar de testar."
adb logcat | grep -E "(RetroMenu3.*NAV|DOWN:|UP:)"
