# Análise do Projeto Revenger

**Data da análise**: 2026-08-27
**Commit de referência**: `0babeac` (Merge pull request #25 from vinaooo/develop) — `master` local sincronizada com `origin/master`

---

## O que é

**Revenger** é um fork do [Ludere](https://github.com/tytydraco/Ludere) (GPLv3), criado por [tytydraco](https://github.com/tytydraco). É um empacotador Android: ROM + core LibRetro + configuração viram **um único APK autocontido** — abrir o app já é jogar, sem importar ROM nem baixar core manualmente. Todo o comportamento do app é controlado por [`app/src/main/res/values/config.xml`](../app/src/main/res/values/config.xml).

### Filosofia

Reduzir emulação Android a um passo único: instalar o APK. Em vez de abrir um emulador genérico, baixar um core, localizar a ROM e configurar controles, o Revenger já entrega tudo pronto — inclusive facilitando distribuir uma cópia idêntica da configuração para outro dispositivo, bastando instalar o mesmo APK.

### Configuração ativa no momento da análise

| Chave | Valor |
|---|---|
| `conf_id` | *(id do jogo)* |
| `conf_name` | *(título do jogo)* |
| `conf_core` | *(core do console 8-bit configurado)* |
| `conf_rom` | *(arquivo da ROM)* |
| `conf_target_abi` | `arm64-v8a` (build só para essa ABI) |
| `conf_shader` | `settings` (usuário escolhe no menu) |
| `conf_gp_haptic` | `true` |
| `conf_gp_select` | `false` (botão SELECT do gamepad virtual oculto) |

---

## Stack técnica

- **Kotlin** 2.2.20, **AGP** 8.13.0, **Gradle** 8.14
- JVM target 21, `minSdk` 30 (Android 11), `compileSdk`/`targetSdk` 36 (Android 16)
- `viewBinding` habilitado, lint customizado via **detekt** 1.23.6
- Dependências principais ([`app/build.gradle`](../app/build.gradle)):
  - [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) 0.12.0 — frontend que interage com os cores LibRetro
  - [RadialGamePad](https://github.com/Swordfish90/RadialGamePad) 2.0.0 — controles touch virtuais, com suporte nativo a Kotlin Flow
  - RxJava3 (`rxjava`/`rxandroid`) + `kotlinx-coroutines-android`
  - **Sem Material Components** — decisão deliberada; a UI é 100% custom (`RetroCardView`), com estética retrô (sem sombras/cantos/theming Material)
- Testes: JUnit4, Mockito, MockK, Robolectric, `androidx.fragment:fragment-testing`, `kotlinx-coroutines-test`

---

## Build system — dois esquemas dinâmicos

### 1. Download do core LibRetro (`prepareCore`)

Task Gradle que roda antes de `preBuild`. Baixa de `https://buildbot.libretro.com/nightly/android/latest/` apenas a(s) ABI(s) definida(s) em `conf_target_abi` (ou as quatro — `x86`, `x86_64`, `armeabi-v7a`, `arm64-v8a` — se o valor for `"all"`). Antes de baixar, faz uma requisição `HEAD` para checar se o core existe para aquela ABI (evita build quebrado por 404); se a pasta ficar vazia, ela é removida para não gerar um split de ABI vazio. Pula o download se `jniLibs/<abi>` já tiver conteúdo.

### 2. Staging de ROMs (`prepareRom`)

Task que roda antes de `prepareCore`. As ROMs **não ficam no controle de versão** — moram em **`roms_backup/`, na raiz do projeto** (`rootProject.projectDir`, fora de `app/`), pasta listada no `.gitignore` como `/roms_backup`. A cada build:

1. Qualquer ROM presente em `app/src/main/assets/rom/` que não seja a ativa (`conf_rom`) é devolvida para `roms_backup/`.
2. A ROM ativa é movida de `roms_backup/` para `app/src/main/assets/rom/` (destino real de empacotamento, hoje com apenas um `.gitkeep` versionado).

```groovy
task prepareRom {
    doLast {
        def romName = getConfigValue('conf_rom')
        def romDir = file("${rootProject.projectDir}/app/src/main/assets/rom")
        def backupDir = file("${rootProject.projectDir}/roms_backup")
        // 1. tira do assets/rom/ tudo que não for a ROM ativa -> roms_backup/
        // 2. traz a ROM ativa de roms_backup/ -> assets/rom/
    }
}
prepareCore.dependsOn prepareRom
preBuild.dependsOn prepareCore
```

**Uso prático**: todas as ROMs ficam soltas em `roms_backup/` (fora do git); trocar de jogo é só editar `conf_rom` em `config.xml` — o staging é automático. Isso resolve dois problemas: repositório/APK enxutos (nenhuma ROM commitada) e troca de jogo sem copiar/colar arquivos manualmente.

> **Nota de nomenclatura**: as chaves do `config.xml` migraram de `config_*` para `conf_*` (ex.: `config_rom` → `conf_rom`, `config_gamepad_a` → `conf_gp_a`). O arquivo [`.github/copilot-instructions.md`](../.github/copilot-instructions.md) ainda documenta a nomenclatura antiga (`config_*`) e o local antigo da ROM (`res/raw/`) — está desatualizado em relação ao código atual.

---

## Arquitetura de código

**96 arquivos Kotlin, ~23.000 linhas**, padrão MVVM.

```
SplashActivity (entry point)  → boot com efeito CRT (ui/splash/CRTBootView)
      ↓
GameActivity ──> GameActivityViewModel (god object, ~1800 linhas)
   ├── RetroView                 (superfície LibretroDroid)
   ├── GamePad / GamePadConfig   (RadialGamePad)
   ├── ControllerInput           (combo SELECT+START, debounce, grace period)
   ├── OrientationManager        (lógica de orientação extraída da Activity)
   ├── managers/
   │    ├── SaveStateManager     (CRUD de save states multi-slot)
   │    └── SessionSlotTracker
   ├── models/
   │    └── SaveSlotData
   └── ui/retromenu3/            (sistema de menus — Command Pattern + State Machine)
        ├── RetroMenu3Fragment + Progress/Settings/About/Exit/
        │     ManageSaves/SaveSlots Fragments
        ├── MenuSystem.kt        (MenuAction, MenuEvent, MenuState)
        ├── managers especializados: Lifecycle, StateController, ViewInitializer,
        │     Animation, CallbackManager, ActionHandler, SubmenuCoordinator
        ├── callbacks/           (ManageSavesListener, SaveSlotsListener, SaveStateOperations)
        └── navigation/          (NavigationController = single source of truth)
```

### Sistema de menus (RetroMenu3)

Refatorado seguindo SOLID/SRP: o `RetroMenu3Fragment` atua como **coordenador**, delegando responsabilidades a managers especializados (ver [`definitions/RetroMenu3_code.md`](../definitions/RetroMenu3_code.md)). Usa:
- **Command Pattern**: `MenuAction` (sealed class) para ações type-safe
- **State Machine**: `MenuState` enum centralizando navegação
- UI 100% custom com `RetroCardView`, sem Material Design

### Sistema de navegação multi-input

Todos os inputs — gamepad virtual, gamepad físico, touch, teclado — são traduzidos para `NavigationEvent` unificados e processados pelo `NavigationController`. Arquitetura resultado do refactor documentado em [`docs/MULTI_INPUT_NAVIGATION_REFACTOR_PLAN.md`](MULTI_INPUT_NAVIGATION_REFACTOR_PLAN.md) (concluído; feature flags já removidas, `FeatureFlags.kt` é hoje um objeto vazio). Debounce: 30ms para navegação, 200ms para ações; grace period de 200ms após fechar menu.

### Novidades recentes (PRs #20–#25)

- **Splash com animação CRT**: `SplashActivity` como novo launcher (`AndroidManifest.xml`) e `CRTBootView` (os efeitos de fundo antigos em `ui/effects/` foram removidos como código morto, PR #128). `GameActivity` deixou de ser `exported`.
- **Sistema de save multi-slot**: substituiu o slot único — múltiplos slots de save/state, tela de gerenciamento (`ManageSavesFragment`, `SaveSlotsFragment`), preview em tela cheia dos load states, screenshot com auto-crop de bordas pretas, teclado retrô para renomear slots.
- **`OrientationManager`**: lógica de orientação extraída da `GameActivity` para uma classe dedicada.
- Ajustes finos de alinhamento do gamepad virtual (dials vazios simétricos).

---

## Pontos de atenção / débito técnico

- **Código morto**: pacote `ui/retromenu3/navigation/adapters/` (`GamepadInputAdapter`, `TouchInputAdapter`, `KeyboardInputAdapter`) não é referenciado em lugar nenhum — e coexiste com **outra classe `KeyboardInputAdapter`**, de mesmo nome, em `navigation/` direto (essa sim é a usada por `GameActivityViewModel`).
- [`ControllerInput.kt.broken`](../app/src/main/java/com/vinaooo/revenger/input/ControllerInput.kt.broken) segue versionado no repositório.
- `MenuPerformanceBenchmark.kt` existe tanto em `main/` quanto em `test/` — a versão em `main/` é infraestrutura de benchmark que não deveria estar em código de produção.
- 9 arquivos ainda contêm `TODO`/`FIXME` (destaque: `MenuViewModel` e `MenuInputHandler` têm métodos stub não implementados).
- `docs/` inteiro é gitignored — todo o planejamento (incluindo este arquivo) fica só local, não chega ao GitHub.
- `autogen/` é citado no README e no workflow [`.github/workflows/autogen.yml`](../.github/workflows/autogen.yml), mas a pasta não existe no repositório — esse workflow quebra se disparado.
- `.gitignore` tem padrões amplos no fim (`*test*`, `*log*`, `*debug*`) — um arquivo de teste novo não entra no git sem `git add -f`.
- `revenger.jks` (keystore de release) está commitado, com senha pública documentada no README (herdado do Ludere).

---

## Referências internas

- [README.md](../README.md) — visão geral, filosofia, instruções de build e configuração
- [`.github/copilot-instructions.md`](../.github/copilot-instructions.md) — guia para assistentes de IA (parcialmente desatualizado quanto a `config_*`/`res/raw/`)
- [`definitions/RetroMenu3_code.md`](../definitions/RetroMenu3_code.md) — arquitetura detalhada do sistema de menus
- [`docs/MULTI_INPUT_NAVIGATION_REFACTOR_PLAN.md`](MULTI_INPUT_NAVIGATION_REFACTOR_PLAN.md) — plano completo do refactor de navegação multi-input
- [`docs/PROJECT_STATUS.md`](PROJECT_STATUS.md) — snapshot de progresso de uma fase anterior do refactor de navegação
