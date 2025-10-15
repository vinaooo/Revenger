#!/bin/bash

# Script para capturar logs durante teste de navegação de submenu
# Uso: ./test_submenu_navigation.sh

echo "=== TESTE DE NAVEGAÇÃO DE SUBMENU ==="
echo "Iniciando captura de logs..."
echo "Pressione Ctrl+C para parar"
echo ""

# Criar arquivo de log com timestamp
LOG_FILE="submenu_navigation_test_$(date +%Y%m%d_%H%M%S).txt"

echo "Logs serão salvos em: $LOG_FILE"
echo "=== INÍCIO DO TESTE ===" > "$LOG_FILE"

# Filtrar apenas logs relacionados ao submenu
adb logcat -v time -s "RetroMenu3" "SUBMENU" | tee -a "$LOG_FILE"