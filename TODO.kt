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
