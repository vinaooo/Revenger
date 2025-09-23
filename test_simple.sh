#!/bin/bash

# Script de teste simplificado para gamepad
# Autor: vinaooo
# Data: 22 de Setembro de 2025

echo "🎮 TESTE SIMPLIFICADO DE GAMEPAD"
echo "================================"

# Diretórios
BASE_DIR="/home/vina/Projects/Emuladores/Revenger"
CONFIG_DIR="$BASE_DIR/config_backup"
TARGET_DIR="$BASE_DIR/app/src/main/res/values"

cd "$BASE_DIR" || exit 1

# Função para verificar dispositivo
check_device() {
    echo "📱 Verificando dispositivo Android..."
    if ! command -v adb &> /dev/null; then
        echo "❌ ADB não encontrado. Instale o Android SDK."
        return 1
    fi
    
    local devices=$(adb devices | grep -v "List of devices" | grep -v "^$" | wc -l)
    if [ "$devices" -eq 0 ]; then
        echo "⚠️  Nenhum dispositivo Android conectado."
        echo "   Conecte um dispositivo com USB Debug ativado."
        return 1
    fi
    
    echo "✅ Dispositivo Android encontrado!"
    return 0
}

# Função para build
build_project() {
    echo "🔨 Compilando projeto..."
    if ./gradlew assembleDebug --quiet; then
        echo "✅ Build bem-sucedido!"
        return 0
    else
        echo "❌ Falha no build"
        return 1
    fi
}

# Função para instalar
install_apk() {
    local apk_path="$BASE_DIR/app/build/outputs/apk/debug/app-debug.apk"
    
    if [ ! -f "$apk_path" ]; then
        echo "❌ APK não encontrado: $apk_path"
        return 1
    fi
    
    echo "📱 Instalando APK..."
    if adb install -r "$apk_path"; then
        echo "✅ APK instalado com sucesso!"
        return 0
    else
        echo "❌ Falha na instalação"
        return 1
    fi
}

# Função para aplicar configuração
apply_config() {
    local config_name="$1"
    local config_file="$CONFIG_DIR/config_${config_name}.xml"
    local target_file="$TARGET_DIR/config.xml"
    
    echo "📁 Aplicando configuração: $config_name"
    
    if [ ! -f "$config_file" ]; then
        echo "❌ Arquivo não encontrado: $config_file"
        return 1
    fi
    
    if cp "$config_file" "$target_file"; then
        echo "✅ Configuração $config_name aplicada!"
        return 0
    else
        echo "❌ Falha ao copiar configuração"
        return 1
    fi
}

# Função de teste completo
test_config() {
    local config_name="$1"
    local game_name="$2"
    
    echo ""
    echo "🎯 TESTANDO: $game_name"
    echo "========================"
    
    if apply_config "$config_name"; then
        if build_project; then
            if check_device; then
                if install_apk; then
                    echo "🚀 Teste de $game_name concluído!"
                    echo "   Abra o app no dispositivo para testar."
                    return 0
                fi
            fi
        fi
    fi
    
    echo "❌ Teste de $game_name falhou!"
    return 1
}

# Menu principal
show_menu() {
    echo ""
    echo "OPÇÕES DISPONÍVEIS:"
    echo "1) Sonic The Hedgehog (Master System)"
    echo "2) Rock & Roll Racing (SNES)"  
    echo "3) Legend of Zelda (Game Boy)"
    echo "4) Sonic & Knuckles (Mega Drive)"
    echo "5) Apenas Build (sem trocar config)"
    echo "6) Verificar dispositivo"
    echo "0) Sair"
    echo ""
}

# Loop principal
while true; do
    show_menu
    echo -n "Escolha uma opção (0-6): "
    read -r choice
    
    case $choice in
        1)
            test_config "sth" "Sonic The Hedgehog"
            ;;
        2)
            test_config "rrr" "Rock & Roll Racing"
            ;;
        3)
            test_config "loz" "Legend of Zelda"
            ;;
        4)
            test_config "sak" "Sonic & Knuckles"
            ;;
        5)
            echo "🔨 Executando build apenas..."
            if build_project; then
                if check_device; then
                    install_apk
                fi
            fi
            ;;
        6)
            check_device
            ;;
        0)
            echo "👋 Encerrando..."
            break
            ;;
        *)
            echo "❌ Opção inválida! Use 0-6."
            ;;
    esac
    
    echo ""
    echo "Pressione ENTER para continuar..."
    read -r
done

echo "Teste finalizado!"
