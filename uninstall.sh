
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