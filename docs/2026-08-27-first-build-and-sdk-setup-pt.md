# Build & Deploy — Revenger — 2026-08-27

## Resultado

✅ APK debug compilado e instalado no dispositivo. O jogo configurado rodando no dispositivo físico (arm64).

---

## 1. Jogo programado no momento

Fonte: [`app/src/main/assets/config/config.json`](../app/src/main/assets/config/config.json)

| Chave | Valor |
|---|---|
| `rom` | *(arquivo da ROM configurada)* |
| `platform` | *(id de um console 8-bit)* |
| `name` | *(título configurado)* |
| `default_settings` | `true` |
| `target_abi` | `arm64-v8a` |

Como `default_settings = true`, a config de gameplay vem do perfil da `platform` configurada em
[`app/src/main/assets/default_settings.json`](../app/src/main/assets/default_settings.json)
(**não** do `config_manual.json`, que está inativo):

- **Core:** o core do perfil
- **Variables:** uma opção do core (`<prefixo-do-core>_hide_left_border=enabled`)
- **Orientação:** landscape · **Shader:** `upscale1` · **Fullscreen:** true · **PiP:** on
- **Fast-forward:** 2x · **Menu:** `combo,gamepad,back`, FAB em `bottom-right`
- **Gamepad virtual:** on, sem háptico; A, B, START, SELECT visíveis; X/Y, ombros e analógico off

Package Android gerado: `com.vinaooo.revenger.<name>_<core>`
(`applicationId` = `com.vinaooo.revenger.${generateConfigId(name, core)}` → `<name>_<core>`)

---

## 2. Bloqueio encontrado: sem Android SDK

- `local.properties` (`sdk.dir`) e **todas** as configs do Android Studio apontam para
  `/home/vina/Android/Sdk` — **essa pasta não existia** (nem em snapshots do Timeshift).
- Só havia `platform-tools` (`adb`) em `/usr/lib/android-sdk`. Faltavam platform 36,
  build-tools, `sdkmanager`, `aapt2`.
- **As 3 instalações do Android Studio não contêm SDK** (o AS nunca embute o SDK,
  baixa em `~/Android/Sdk` no primeiro uso):

  | Caminho | Versão |
  |---|---|
  | `/home/vina/Development/android-studio` | 2022.3.1 (Giraffe) |
  | `/opt/android-studio` | 2024.3.2 |
  | Toolbox `android-studio-2` | 2026.1 (mais nova) |

  Para build por linha de comando (Gradle wrapper) a versão do AS é irrelevante;
  o que faltava era o SDK em si.

---

## 3. Correção aplicada

Instalado o SDK em `/home/vina/Android/Sdk` (o caminho que tudo já esperava):

1. Download do `commandlinetools-linux` oficial da Google →
   `/home/vina/Android/Sdk/cmdline-tools/latest`
2. Licenças do Android SDK aceitas (`sdkmanager --licenses`)
3. Componentes instalados via `sdkmanager`:
   - `platform-tools`
   - `platforms;android-36`  (casa com `compileSdk = 36`)
   - `build-tools;36.0.0`     (AGP 8.13.0)

Toolchain já presente e usada: Gradle wrapper 8.14, JDK 21
(`/opt/android-studio/jbr` ou system Java 21.0.4).

---

## 4. Build & install

```bash
./gradlew installDebug
```

- `BUILD SUCCESSFUL in 22s`
- `prepareRom` fez staging da ROM ativa para `app/src/main/assets/rom/`
- `cleanupRom` devolveu a ROM para `roms_backup/` ao final (estado do repo preservado)
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- `Installed on 1 device` → *(dispositivo físico, Android 16)*

Launch:

```bash
adb shell monkey -p com.vinaooo.revenger.<name>_<core> -c android.intent.category.LAUNCHER 1
```

`topResumedActivity = com.vinaooo.revenger.<name>_<core>/com.vinaooo.revenger.views.GameActivity`

Screenshot confirmou: tela de título do jogo,
landscape, gamepad virtual (D-pad + A/B + FAB de menu bottom-right + botões +/- de velocidade).

---

## 5. Dispositivo

| Campo | Valor |
|---|---|
| ADB | `<ip>:<porta>` (Wi-Fi) |
| Modelo | smartphone físico, arm64 — casa com `target_abi: arm64-v8a` |
| Android | API 16-alvo mostrado como "16" no ADB (build target) |

---

## 6. Nenhuma alteração no projeto

O código-fonte e os arquivos de config **não foram modificados**. As mudanças em
`git status` (`assets/config/*.json`, `default_settings.json`, `.kt`, layouts) já
existiam antes desta sessão. O staging de ROM feito pelo build foi automaticamente revertido.

## 7. Se o SDK sumir de novo

Reexecutar o passo 3 (download do commandlinetools + `sdkmanager` dos 3 componentes).
Registrado em memória: `android-sdk-setup`.
