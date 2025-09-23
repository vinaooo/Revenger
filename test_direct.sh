#!/bin/bash

# Teste manual direto - sem automação complexa
echo "🎮 TESTE MANUAL DIRETO"
echo "======================"

cd /home/vina/Projects/Emuladores/Revenger

echo "1. Verificando arquivos de configuração..."
ls -la config_backup/config_*.xml

echo ""
echo "2. Testando build..."
if ./gradlew assembleDebug --quiet --console=plain; then
    echo "✅ Build OK!"
else
    echo "❌ Build falhou!"
    exit 1
fi

echo ""
echo "3. Verificando APK gerado..."
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ APK encontrado!"
    ls -lh app/build/outputs/apk/debug/app-debug.apk
else
    echo "❌ APK não encontrado!"
    exit 1
fi

echo ""
echo "4. Verificando ADB..."
if command -v adb &> /dev/null; then
    echo "✅ ADB disponível"
    echo "Dispositivos conectados:"
    adb devices
else
    echo "❌ ADB não encontrado"
fi

echo ""
echo "🚀 TUDO PRONTO!"
echo "Para testar manualmente:"
echo "1. Conecte dispositivo Android (USB Debug)"
echo "2. Execute: adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo "3. Abra o app no dispositivo"
