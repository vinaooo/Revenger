#!/bin/bash

#!/bin/bash

# Package list
# PACKAGES=(
#     com.vinaooo.revenger.sak
#     com.vinaooo.revenger.sth
#     com.vinaooo.revenger.loz
#     com.vinaooo.revenger.rrr
# )

# for pkg in "${PACKAGES[@]}"; do
#     # If installed, try to uninstall
#     if adb shell pm list packages "$pkg" | grep -q "$pkg"; then
#     adb uninstall "$pkg" \
#         || adb shell pm uninstall --user 0 "$pkg"
#     fi
# done

cp ./config_backup/config_rock_snes.xml ./app/src/main/res/values/config.xml
./gradlew clean assembleDebug installDebug

cp ./config_backup/config_sonic_md.xml ./app/src/main/res/values/config.xml
./gradlew clean assembleDebug installDebug

cp ./config_backup/config_sonic_ms.xml ./app/src/main/res/values/config.xml
./gradlew clean assembleDebug installDebug

cp ./config_backup/config_zelda_gb.xml ./app/src/main/res/values/config.xml
./gradlew clean assembleDebug installDebug

cp ./config_backup/config_shinobi_ms.xml ./app/src/main/res/values/config.xml
./gradlew clean assembleDebug installDebug

cp ./config_backup/config_sonic_ms_vinicius.xml ./app/src/main/res/values/config.xml
./gradlew clean assembleDebug installDebug