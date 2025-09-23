#!/bin/bash

# Script rápido para trocar ROMs - FASE 4 PadKit Universal
echo "🎮 Troca Rápida de ROM - Sistema PadKit Universal"

case "$1" in
    "sth")
        echo "🦔 Trocando para: Sonic The Hedgehog (Master System)"
        cp config_backup/config_sth.xml app/src/main/res/values/config.xml
        ;;
    "rrr") 
        echo "🏎️ Trocando para: Rock and Roll Racing (Super Nintendo)"
        cp config_backup/config_rrr.xml app/src/main/res/values/config.xml
        ;;
    "loz")
        echo "🗡️ Trocando para: Legend of Zelda (Game Boy)" 
        cp config_backup/config_loz.xml app/src/main/res/values/config.xml
        ;;
    "sak")
        echo "🌟 Trocando para: Sonic and Knuckles (Mega Drive)"
        cp config_backup/config_sak.xml app/src/main/res/values/config.xml
        ;;
    *)
        echo "❌ ROM inválida. Use: sth, rrr, loz ou sak"
        echo ""
        echo "Exemplos:"
        echo "  ./quick_rom.sh sth  # Sonic The Hedgehog"
        echo "  ./quick_rom.sh rrr  # Rock and Roll Racing"
        echo "  ./quick_rom.sh loz  # Legend of Zelda"
        echo "  ./quick_rom.sh sak  # Sonic and Knuckles"
        exit 1
        ;;
esac

echo "✅ ROM aplicada! Execute: ./gradlew assembleDebug"
