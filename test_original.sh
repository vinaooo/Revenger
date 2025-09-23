#!/bin/bash

./gradlew clean
cp ./config_backup/config_rock_snes.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
adb install ./app/build/outputs/apk/debug/app-debug.apk

./gradlew clean
cp ./config_backup/config_sonic_md.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
adb install ./app/build/outputs/apk/debug/app-debug.apk

./gradlew clean
cp ./config_backup/config_sonic_ms.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
adb install ./app/build/outputs/apk/debug/app-debug.apk

./gradlew clean
cp ./config_backup/config_zelda_gb.xml ./app/src/main/res/values/config.xml
./gradlew assembleDebug
adb install ./app/build/outputs/apk/debug/app-debug.apk
