# Plano de Implementação dos TODOs — RetroMenu3 / Navegação

Este documento é o plano focado exclusivamente na implementação dos TODOs que foram removidos do plano de alto nível de refatoração (`plan-navigationRefactorPlan.prompt.md`). Ele contém: lista priorizada de TODOs, arquivos afetados, critérios de aceitação (AC), passos incrementais de implementação, dependências, testes de aceitação, e estimativas.

Objetivo: centralizar todo o trabalho técnico (mudanças de código, testes e validações) necessário para resolver os TODOs, dividido em pacotes de trabalho e pronto para execução em PRs incrementalmente.

---

## Estrutura do documento
- Resumo e prioridades
- Pacotes de trabalho (Work Packages)
- Entrada detalhada por TODO (template preenchível)
- Checklists para PRs
- Testes e scripts para validação

---

## Resumo e Prioridades (Visão curta)
- Priority High: Corretivas e segurança (‟MenuStateController` duplication, `GLOBAL_STATE_LOCK` logic), Input wiring
- Priority Medium: ViewModels e ações de menu (mostrar/fechar/dismissAll, fast-forward, audio), GamePad setup
- Priority Low: Arquitetura e simplificações (extrair `NavigationStateManager`, `NavigationEventProcessor`, `MenuManagerFactory`)

---

## Pacotes de Trabalho (Work Packages)
- Pacote A — Correções críticas (alto):
  - Remover `init` duplicado em `MenuStateController` (bug e duplicação de `menuItems`).
  - Garantir decisão do keyboard logic ocorra dentro de `GLOBAL_STATE_LOCK.withLock`.

- Pacote B — Input wiring (alto):
  - Implementar `MenuInputHandler.setupInputHandling()` e realizar wiring do RadialGamePad + entradas físicas (teclado, gamepads).
  - Implementar `clearControllerInputState()` para reset de logs e flags.

- Pacote C — ViewModels e menu flow (médio):
  - `MenuViewModel` — Implementar `showRetroMenu()`, `dismissRetroMenu()` e `dismissAllMenus()`.
  - `GameStateViewModel` — Implementar `save`, `load`, `hasSave`.
  - `SpeedViewModel`, `AudioViewModel` — Implement toggles e integração com MenuCallbackManager.

- Pacote D — Split NavigationController (médio/alto):
  - Extrair `NavigationStateManager` (estado) e `NavigationEventProcessor` (processamento). Ver manutenção do public API.

- Pacote E — Refactor & Simplificação (médio/baixo):
  - Criar `MenuManagerFactory` e simplificar `RetroMenu3Fragment.initializeManagers()`.

---

## Template detalhado por TODO
Use este template para cada TODO que precisa ser implementado.

TODO: {texto exato do TODO}
Arquivos afetados: `path/arquivoA`, `path/arquivoB`
Prioridade: (Alta | Média | Baixa)
Estimativa: (XS | S | M | L)
Descrição: {descrição do que está faltando}
AC (Acceptance Criteria):
- {AC 1}
- {AC 2}
Passos de implementação (incrementais):
1. Implementar mínima mudança não-intrusiva e cobrir com testes unitários
2. Implementar integração com outras camadas (ViewModel, Controller)
3. Adicionar testes de integração/manual script sob `tests/` e validar
4. Submeter PR com checklist e docs atualizadas
Dependências: {ex.: item X do pacote A}
Testes de validação: `tests/test_keyboard_navigation.sh`, `tests/test_*` relevantes
Notas: {qualquer observação extra}

---

## TODOs Prioritizados e Entradas (preenchidas)

### 1) TODO: Implementação básica do MenuInputHandler. TODO: Implementar lógica completa após criar MenuCallbackManager.
- Arquivos afetados: `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/MenuInputHandler.kt`
- Prioridade: Alta
- Estimativa: M
- Descrição: `MenuInputHandler` possui implementação básica; falta `setupInputHandling()` para registrar listeners e traduzir eventos para NavigationController / MenuActionHandler.
- AC:
  - `setupInputHandling()` registra callbacks para RadialGamePad, virtual buttons e entradas físicas
  - Ao apertar B/selector, navigate/activate deve funcionar (testado em `tests/test_radial_gamepad_events.sh`)
- Passos:
  1. Revisar `MenuCallbackManager` e confirmar API de callbacks
  2. Implementar `setupInputHandling()` e testes unitários
  3. Rodar `tests/test_radial_gamepad_events.sh` e ajustar
- Dependências: `MenuCallbackManager`, `MenuActionHandler`
- Tests de validação: `tests/test_radial_gamepad_events.sh`, `tests/test_button_b_multiple.sh`

### 2) TODO: // TODO: Configurar listeners para botões virtuais e entrada física
- Arquivos afetados: `MenuInputHandler.kt`
- Prioridade: Alta
- Estimativa: S
- Descrição: Conferir IDs/Layouts de botões virtuais e GamePad adapters; registrar os listeners que chamam `MenuInputHandler.handle*()`.
- AC:
  - Ações de toque e eventos de hardware são roteados corretamente para NavigationController
- Passos: 1) Identificar e documentar callbacks existentes 2) Registrar 3) Testes E2E
- Dependências: `RadialGamePad`, `InputViewModel`

### 3) TODO: // TODO o código de decisão DEVE estar dentro do withLock
- Arquivos afetados: `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/navigation/KeyboardInputAdapter.kt`
- Prioridade: Alta
- Estimativa: S
- Descrição: As decisões de dedup e press-cycle devem ser protegidas pelo lock global.
- AC:
  - O código crítico executa dentro de `GLOBAL_STATE_LOCK.withLock{}`, sem comportamento race durante testes multi-input
- Passos: 1) Revisar código 2) Mover blocos de decisão para dentro do lock 3) Adicionar testes unitários / multi-thread aceitação 4) Remover TODO
- Testes: `tests/test_single_trigger_v4.2.sh`

### 4) TODO: Remover bloco `init` duplicado no `MenuStateController`
- Arquivos: `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/MenuStateController.kt`
- Prioridade: Alta
- Estimativa: XS
- Descrição: `initializeMenuItems()` está sendo chamado duas vezes; remover duplicidade e adicionar teste
- AC:
  - `menuItems.size` == expected count
  - nenhum item duplicado
- Passos: 1) Remover 2º bloco `init` 2) Adicionar unit test validate menu items 3) Validar `tests/test_menu_state_bug.sh`
- Testes: `tests/test_menu_state_bug.sh`, `tests/test_rotation_complete.sh`

Status: ✅ Implementado
Branch: `fix/remove-dup-init-menu-state-controller`
Commit: Fix(MenuStateController): remove duplicate init and add unit tests to prevent duplication

### 5) TODO: Implementar lógica completa de mostrar menu (MenuViewModel)
- Arquivos: `app/src/main/java/com/vinaooo/revenger/viewmodels/MenuViewModel.kt`
- Prioridade: Alta
- Estimativa: M
- Descrição: `showRetroMenu()` deve acionar `NavigationController` para abrir o menu e atualizar estado do viewModel
- AC:
  - Ao chamar showRetroMenu, menu é aberto, seleção inicial é consistente, tests de integração passam
- Passos: 1) Implementar chamada para `navigationController` 2) Atualizar state + testes unitários 3) Testes de integração
- Testes: `tests/test_keyboard_navigation.sh`, `tests/test_back_button_state.sh`

### 6) TODO: Implementar lógica de fechar menu e fechar todos (MenuViewModel)
- Arquivos: `MenuViewModel.kt`
- Prioridade: Alta
- Estimativa: S
- Descrição: `dismissRetroMenu()` e `dismissAllMenus()` devem fechar menus e limpar estados adequadamente
- AC:
  - Ao fechar, overlay finaliza e `InputViewModel.clearControllerInputState()` é chamado
- Passos: 1) Implementar fechamento com `CloseAllMenus` event 2) Validar testes scripts 3) Programar `onMenuClosedCallback` reset

### 7) TODO: Implementar limpeza do estado de input do controlador e reset combo flag
- Arquivos: `InputViewModel.kt`, `MenuViewModel.kt`
- Prioridade: Alta
- Estimativa: S
- Descrição: Limpar logs e flags para evitar combos persistentes
- ACs:
  - Ao fechar menu, `clearKeyLog()` e `resetComboAlreadyTriggered()` chamadas
  - Reabrir menu com combo funciona (SELECT+START) após fechar
- Passos: 1) Implementar funções 2) Testes unitários 3) End-to-end
- Tests: `tests/test_single_trigger_v4.2.sh`

### 8) TODO: Implementar condicional `controllerInput.shouldHandleSelectStartCombo`
- Arquivos: `InputViewModel.kt`
- Prioridade: Média
- Estimativa: S
- Descrição: Evitar combo quando menus estão abertos ou `isDismissingAllMenus`.
- ACs: `shouldHandleSelectStartCombo` retorna false quando necessário.
- Passos: Implementar condicional; adicionar testes unitários; executar `tests/test_menu_state_bug.sh`.

### 9) TODO: Gamepad setup (`setupGamePads()`)
- Arquivos: `app/src/main/java/com/vinaooo/revenger/viewmodels/InputViewModel.kt`
- Prioridade: Média
- Estimativa: M
- Descrição: Criar e configurar gamepads com callbacks para `MenuInputHandler`
- ACs: GamePad callbacks acionam ações de navegação; scripts `tests/test_radial_gamepad_events.sh` funcionam.
- Passos: Implementar; testar; documentar.

### 10) TODO: SpeedViewModel/AudioViewModel - toggles e aplicações
- Arquivos: `SpeedViewModel.kt`, `AudioViewModel.kt`
- Prioridade: Média
- Estimativa: S
- Descrição: Implementar toggles de fast-forward e áudio, integrando com `MenuCallbackManager`.
- ACs: Ao tocar o toggle no menu, o `RetroView` altera a velocidade/áudio corretamente.
- Passos: Implementar métodos; ligar `MenuCallbackManager`; testes e2e.

### 11) TODO: GameState save/load
- Arquivos: `GameStateViewModel.kt`
- Prioridade: Média
- Estimativa: M
- Descrição: Implementar save/load e checking de save slots; usadas por `ProgressFragment`.
- ACs: Save/Load funcionam e menus exibem slots corretamente.
- Passos: Implementar; adicionar testes unitários; e2e com `tests/test_load_state_removal.sh` e `tests/test_rotation_complete.sh`.

### 12) TODO (Arquitetura): Dividir `NavigationController`
- Arquivos: `NavigationController.kt` + novas `NavigationStateManager.kt`, `NavigationEventProcessor.kt`
- Prioridade: Média/Alto
- Estimativa: L
- Descrição: Refatorar para reduzir complexidade e melhorar testabilidade. Fazer em PRs pequenos.
- ACs: API preservada; testes unitários existentes continuam passando.
- Passos: 1) Criar `NavigationStateManager` com leitura e escrita de estado 2) Move pequenas porções do `handleNavigationEvent` para `NavigationEventProcessor` 3) Adição de testes unitários

### 13) TODO: Criar `MenuManagerFactory` e simplificar fragment initialization
- Arquivos: `RetroMenu3Fragment.kt`, `MenuManagerFactory.kt`
- Prioridade: Baixa/Média
- Estimativa: M
- Descrição: Centralizar criação e injeção manual das managers
- ACs: `initializeManagers()` simplificado; fábricas testáveis.
- Passos: 1) Criar interface 2) Refatorar fragment 3) Ajustar docs e tests

---

## PR Checklist (para cada PR por TODO/Pacote)
- [ ] Unit tests updated or added
- [ ] E2E/script tests updated or add (under `tests/`)
- [ ] Documentation updated (`docs/` or `definitions/`)
- [ ] Changelog / PR description includes summary of change and risk
- [ ] CI green
- [ ] Code style & linting ok

---

## Test Scripts e validações (mapping rápido)
- MenuInput wiring: `tests/test_radial_gamepad_events.sh`, `tests/test_button_b_multiple.sh`
- Keyboard behavior and single-trigger: `tests/test_single_trigger_v4.2.sh`, `tests/test_keyboard_navigation.sh`
- Rotation & state: `tests/test_rotation_complete.sh`, `tests/test_menu_state_bug.sh`

---

## Notas finais
- Recomendo criar *PRs incrementais por pacote* (Pacote A, Pacote B etc.) porque mudanças grandes (ex.: divisão do controller) são arriscadas. Cada PR deve conter testes que cobrem a mudança introduzida.
- Esse arquivo ficará em `.github/prompts/` e servirá como referência para o time de execução.
- Posso agora: (A) atualizar o plano principal removendo instruções de implementação, (B) gerar rascunho de PRs com titles/descriptions/checklists, (C) abrir issues por TODO ou per-package. Escolha uma.

---

*Arquivo gerado automaticamente por Copilot como rascunho inicial para refinamento.*
