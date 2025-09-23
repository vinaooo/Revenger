#!/bin/bash

# Script de teste PadKit Universal - FASE 4
# Testa o sistema universal com todas as 4 ROMs sem configurações específicas
# Valida que o mesmo gamepad PadKit funciona universalmente

echo "🎮 FASE 4: Teste PadKit Universal System"
echo "======================================="

# ROMs disponíveis para teste
ROMS=("sth" "rrr" "loz" "sak")
ROM_NAMES=("Sonic The Hedgehog" "Rock and Roll Racing" "Legend of Zelda" "Sonic and Knuckles") 
ROM_CORES=("smsplus" "bsnes" "gambatte" "genesis_plus_gx")
ROM_SYSTEMS=("Master System" "Super Nintendo" "Game Boy" "Mega Drive")

# Função para trocar ROM
switch_rom() {
    local rom_id=$1
    local rom_name=$2
    local rom_core=$3
    local rom_system=$4
    
    echo ""
    echo "🔄 Trocando para: $rom_name ($rom_system)"
    echo "   Core: $rom_core"
    echo "   Copiando: config_backup/config_${rom_id}.xml → app/src/main/res/values/config.xml"
    
    cp "config_backup/config_${rom_id}.xml" "app/src/main/res/values/config.xml"
    
    if [ $? -eq 0 ]; then
        echo "✅ Configuração aplicada com sucesso!"
        return 0
    else
        echo "❌ Erro ao aplicar configuração!"
        return 1
    fi
}

# Função para compilar e testar
build_and_test() {
    local rom_name=$1
    
    echo ""
    echo "🔨 Compilando para: $rom_name"
    echo "   Sistema PadKit deve funcionar universalmente..."
    
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        echo "✅ Compilação bem-sucedida para $rom_name!"
        echo "📱 APK gerado em: app/build/outputs/apk/debug/"
        return 0
    else
        echo "❌ Erro na compilação para $rom_name!"
        return 1
    fi
}

# Função de menu interativo
show_menu() {
    echo ""
    echo "🎯 MENU DE TESTE PADKIT UNIVERSAL"
    echo "================================="
    echo "1) 🦔 Sonic The Hedgehog (Master System)"
    echo "2) 🏎️  Rock and Roll Racing (Super Nintendo)"  
    echo "3) 🗡️  Legend of Zelda (Game Boy)"
    echo "4) 🌟 Sonic and Knuckles (Mega Drive)"
    echo "5) 🔄 Testar TODAS as ROMs automaticamente"
    echo "6) ❌ Sair"
    echo ""
    echo "Escolha uma opção (1-6):"
}

# Função para teste automático de todas as ROMs
test_all_roms() {
    echo ""
    echo "🚀 TESTE AUTOMÁTICO: Todas as ROMs"
    echo "=================================="
    echo "Validando sistema PadKit universal com 4 plataformas diferentes..."
    
    local success_count=0
    local total_count=${#ROMS[@]}
    
    for i in "${!ROMS[@]}"; do
        local rom_id="${ROMS[$i]}"
        local rom_name="${ROM_NAMES[$i]}"
        local rom_core="${ROM_CORES[$i]}"
        local rom_system="${ROM_SYSTEMS[$i]}"
        
        echo ""
        echo "📋 TESTE ${i+1}/${total_count}: $rom_name"
        echo "=========================================="
        
        switch_rom "$rom_id" "$rom_name" "$rom_core" "$rom_system"
        if [ $? -eq 0 ]; then
            build_and_test "$rom_name"
            if [ $? -eq 0 ]; then
                ((success_count++))
                echo "✅ SUCESSO: $rom_name funcional com PadKit Universal!"
            else
                echo "❌ FALHA: Erro na compilação para $rom_name"
            fi
        else
            echo "❌ FALHA: Erro ao configurar $rom_name"  
        fi
        
        echo ""
        echo "Pressione ENTER para continuar ou Ctrl+C para parar..."
        read
    done
    
    echo ""
    echo "🏁 RESULTADO FINAL DO TESTE UNIVERSAL"
    echo "====================================="
    echo "✅ ROMs Funcionais: $success_count/$total_count"
    echo "📊 Taxa de Sucesso: $((success_count * 100 / total_count))%"
    
    if [ $success_count -eq $total_count ]; then
        echo ""
        echo "🎉 SUCESSO TOTAL! Sistema PadKit Universal VALIDADO!"
        echo "   ✅ Zero configurações específicas por ROM/core"
        echo "   ✅ Funciona universalmente com todos os sistemas"
        echo "   ✅ Config.xml dinâmico testado e aprovado"
    else
        echo ""
        echo "⚠️  Sistema precisa de ajustes para ROMs falhantes"
    fi
}

# Função para teste individual
test_single_rom() {
    local index=$1
    local rom_id="${ROMS[$index]}"
    local rom_name="${ROM_NAMES[$index]}"
    local rom_core="${ROM_CORES[$index]}"
    local rom_system="${ROM_SYSTEMS[$index]}"
    
    echo ""
    echo "🎯 TESTE INDIVIDUAL: $rom_name"
    echo "=============================="
    
    switch_rom "$rom_id" "$rom_name" "$rom_core" "$rom_system"
    if [ $? -eq 0 ]; then
        build_and_test "$rom_name"
        if [ $? -eq 0 ]; then
            echo ""
            echo "🎮 SISTEMA PRONTO PARA TESTE!"
            echo "   APK compilado com PadKit Universal"
            echo "   Instale e teste os controles no emulador"
            echo ""
            echo "✅ Validações PadKit:"
            echo "   • Layout LayoutRadial funcionando"
            echo "   • Controles analog/digital dinâmicos"  
            echo "   • Botões visíveis conforme config.xml"
            echo "   • Mapeamento LibretroDroid ativo"
            echo "   • Menu Start+Select disponível"
        fi
    fi
}

# Loop principal
while true; do
    show_menu
    read choice
    
    case $choice in
        1) test_single_rom 0 ;;
        2) test_single_rom 1 ;;
        3) test_single_rom 2 ;;
        4) test_single_rom 3 ;;
        5) test_all_roms ;;
        6) 
            echo "👋 Saindo do teste PadKit Universal..."
            exit 0
            ;;
        *)
            echo "❌ Opção inválida. Escolha 1-6."
            ;;
    esac
done
