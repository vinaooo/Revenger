#!/bin/bash

# Script para capturar logcat durante teste do menu RetroMenu3
# Uso: ./capture_logcat.sh

echo "=========================================="
echo "   CAPTURA DE LOGCAT - RETROMENU3 TEST"
echo "=========================================="
echo "Instruções:"
echo "1. Execute este script em um terminal"
echo "2. Abra o app no dispositivo"
echo "3. Pressione SELECT+START para abrir o menu"
echo "4. Navegue até um submenu (Settings ou Progress)"
echo "5. Volte ao menu principal"
echo "6. Tente navegar com D-PAD (cima/baixo)"
echo "7. Pressione Ctrl+C para parar a captura"
echo ""

# Criar nome do arquivo de log com timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="retromenu3_debug_${TIMESTAMP}.txt"

echo "Capturando logcat..."
echo "Arquivo de saída: $LOG_FILE"
echo "Filtrando por tags relacionadas ao menu..."
echo ""

# Capturar logcat com filtros específicos para o menu
adb logcat -c  # Limpar buffer anterior
adb logcat -v time \
    | grep -E "(RetroMenu3|MenuManager|SubmenuCoordinator|NAV|SHOW|SUBMENU)" \
    > "$LOG_FILE"

echo ""
echo "Captura interrompida. Arquivo salvo: $LOG_FILE"