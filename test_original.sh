#!/bin/bash

#!/bin/bash

# Lista de pacotes
PACKAGES=(
    com.vinaooo.revenger.sak
    com.vinaooo.revenger.sth
    com.vinaooo.revenger.loz
    com.vinaooo.revenger.rrr
)

for pkg in "${PACKAGES[@]}"; do
    # Se instalado, tenta desinstalar
    if adb shell pm list packages "$pkg" | grep -q "$pkg"; then
    adb uninstall "$pkg" \
        || adb shell pm uninstall --user 0 "$pkg"
    fi
done

./gradlew clean
cp ./config_backup/config_rock_snes.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
./gradlew installDebug
# adb install ./app/build/outputs/apk/debug/app-debug.apk

./gradlew clean
cp ./config_backup/config_sonic_md.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
./gradlew installDebug
# adb install ./app/build/outputs/apk/debug/app-debug.apk

./gradlew clean
cp ./config_backup/config_sonic_ms.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
./gradlew installDebug
# adb install ./app/build/outputs/apk/debug/app-debug.apk

./gradlew clean
cp ./config_backup/config_zelda_gb.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
./gradlew installDebug
# adb install ./app/build/outputs/apk/debug/app-debug.apk
