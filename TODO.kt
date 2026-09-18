// Use this file with TODO Tree VScode extension

// TODO: testar correçao do PiP e prints
// TODO: Criar um shader fake de crt para o menu
// TODO: Relogio, bateria, wifi e dados no menu
// TODO: Estatisticas do jogo
// TODO: Menu de inicio
// TODO: Usar capa do jogo como fundo de menu e opcionalmente o PiP
// TODO: Analógico funcionando em jogos sem suporte a ele
// TODO: Usar a imagem de capa para TV
// TODO: Backup zip
// TODO: Ajustar animação CRT
// TODO: IA para tradução
// TODO: Rodar a partir de container Docker
// TODO: MenuInputHandlerImpl.setupInputHandling (ui/retromenu3/MenuInputHandler.kt) e um stub --
//       configurar listeners de botoes virtuais e input fisico ali, hoje a configuracao passa
//       por NavigationController.
// TODO: Detekt - 37 issues restantes (zero em 22/09), todos concentrados em 4 arquivos grandes;
//       os achados "baratos" (mecanicos/extracao pontual) ja foram fechados nas PRs #62-#69.
//       Cada item abaixo precisa de testes de caracterizacao antes de qualquer split, por ter
//       LargeClass e/ou metodos de alta complexidade ciclomatica:
//       - input/ControllerInput.kt: LargeClass (833/600), TooManyFunctions (18/11), 4
//         dispatchers de alta complexidade (processGamePadButtonEvent 27/15, processKeyEvent
//         30/15, processMotionEvent 15/15), 5 ComplexCondition, ReturnCount nos 3 dispatchers.
//       - viewmodels/GameActivityViewModel.kt: LargeClass (1332/600), TooManyFunctions (95/11),
//         onMenuEvent complexidade 38/15, isAnyMenuActive complexidade 22/15, NestedBlockDepth e
//         ReturnCount em processKeyEvent.
//       - views/GameActivity.kt: LargeClass (957/600), TooManyFunctions (28/11), onCreate com
//         124 linhas, ReturnCount em onUserLeaveHint/maybeEnterPictureInPictureAfterMenuClosed.
//       - ui/retromenu3/navigation/KeyboardInputAdapter.kt: ReturnCount em onKeyDown/onKeyUp.

// FIXME: Melhorar navegação via teclado
// FIXME: KEYCODE_BACK/KEYCODE_ESCAPE podem disparar NavigateBack/CloseAllMenus 2x por
//        toque físico (ui/retromenu3/navigation/KeyboardInputAdapter.kt) -- só KEYCODE_DEL
//        registra actionKeyDownTimestamps no KEY_DOWN, então BACK/ESCAPE caem no fallback
//        de KEY_UP e disparam de novo. Mitigado pelo debounce de 200ms
//        (MENU_CLOSE_DEBOUNCE_MS em input/ControllerInput.kt), só afeta toque mantido além disso.
// FIXME: GameActivityViewModel compõe um InputViewModel com seu próprio ControllerInput
//        órfão -- InputViewModel.getControllerInput() nunca é a instância que processa
//        input real. MenuFragmentBase.kt#onPause() chama
//        inputViewModel.getControllerInput().clearPendingInputsPreserveHeld() na instância
//        errada, deixando a "FIX ERROR 1 - Phase 4.2" (evitar B/Backspace vazando entre
//        transições de submenu) inerte.
// FIXME: GameActivity.onConfigurationChanged dispara a cadeia toda de recriação de menu 2x
//        por rotação -- reapplyOrientation() re-seta requestedOrientation, causando um
//        segundo onConfigurationChanged. As duas cadeias (~1100ms cada) só convergem certo
//        por sorte de timing hoje.
// FIXME: views/menu/RotationMenuStateResolver não mapeia CoreVariablesFragment -- rotacionar
//        com esse submenu aberto cai pro menu principal em vez de recriá-lo. Alcançável pelo
//        usuário: AboutFragment.kt navega para MenuType.CORE_VARIABLES.
// FIXME: GameActivity.kt (createFragmentForRotationState/registerSubmenuAndSyncNavigationAfterRotation)
//        só re-registra Settings/Progress/About/Exit no ViewModel e restaura foco após rotação;
//        SaveSlots/LoadSlots/ManageSaves/ExitSaveGrid ficam sem registro e sem foco restaurado.

// [ ]: Fazer
// [x]: Feito

// VINA:

// HACK: BLABLABLA

// XXX: AOSKDASO
