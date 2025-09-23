
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
    if adb -s 192.168.3.31:40513 shell pm list packages "$pkg" | grep -q "$pkg"; then
    adb -s 192.168.3.31:40513 uninstall "$pkg" \
        || adb -s 192.168.3.31:40513 shell pm uninstall --user 0 "$pkg"
    fi
done