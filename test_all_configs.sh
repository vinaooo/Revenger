#!/bin/bash

echo "=== Testando todas as configurações de jogos ==="

echo "--- 1. Compilando Sonic and Knuckles (Mega Drive) ---"
cp ./config_backup/config_sonic_md.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
adb install ./app/build/outputs/apk/debug/app-debug.apk

echo "--- 2. Compilando Sonic The Hedgehog (Master System) ---"
cp ./config_backup/config_sonic_ms.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
adb install ./app/build/outputs/apk/debug/app-debug.apk

echo "--- 3. Compilando Legend of Zelda (Game Boy) ---"
cp ./config_backup/config_zelda_gb.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
adb install ./app/build/outputs/apk/debug/app-debug.apk

echo "--- 4. Compilando Rock and Roll Racing (SNES) ---"
cp ./config_backup/config_rock_snes.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
adb install ./app/build/outputs/apk/debug/app-debug.apk

echo "=== TODOS OS 4 JOGOS COMPILADOS E INSTALADOS ==="