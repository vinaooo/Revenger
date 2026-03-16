#!/bin/bash

# Update GameStateViewModel
cat app/src/main/java/com/vinaooo/revenger/viewmodels/GameStateViewModel.kt | \
sed -e '/import kotlinx.coroutines.launch/a import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow' \
-e '/private var skipNextTempStateLoad = false/a \
\n    sealed class GameStateEvent {\n        object Idle : GameStateEvent()\n        object ResetGame : GameStateEvent()\n        data class SaveState(val slot: Int) : GameStateEvent()\n        data class LoadState(val slot: Int) : GameStateEvent()\n        data class HasSaveState(val slot: Int) : GameStateEvent()\n        data class SetGameSpeed(val speed: Int) : GameStateEvent()\n    }\n\n    private val _eventFlow = MutableStateFlow<GameStateEvent>(GameStateEvent.Idle)\n    val eventFlow: StateFlow<GameStateEvent> = _eventFlow.asStateFlow()' \
-e 's/\/\/ TODO: Implement game reset logic/\_eventFlow.value = GameStateEvent.ResetGame/g' \
-e 's/\/\/ - Save temporary state if needed//g' \
-e 's/\/\/ - Reset the emulator//g' \
-e 's/\/\/ - Restore settings//g' \
-e 's/\/\/ TODO: Implement save state/\_eventFlow.value = GameStateEvent.SaveState(slot)/g' \
-e 's/\/\/ TODO: Implement load state/\_eventFlow.value = GameStateEvent.LoadState(slot)/g' \
-e 's/\/\/ TODO: Check if save state exists/\_eventFlow.value = GameStateEvent.HasSaveState(slot)/g' \
-e 's/\/\/ TODO: Implement game speed configuration/\_eventFlow.value = GameStateEvent.SetGameSpeed(speed)/g' \
-e 's/\/\/ Validate range (normally 1-2)//g' \
-e 's/\/\/ Apply to emulator//g' > tmp_game_state.kt
mv tmp_game_state.kt app/src/main/java/com/vinaooo/revenger/viewmodels/GameStateViewModel.kt

# Update AudioViewModel
cat app/src/main/java/com/vinaooo/revenger/viewmodels/AudioViewModel.kt | \
sed -e '/import kotlinx.coroutines.launch/a import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow' \
-e '/private var isAudioEnabled: Boolean = true/a \
\n    sealed class AudioEvent {\n        object Idle : AudioEvent()\n        data class ToggleAudio(val retroView: Any?) : AudioEvent()\n    }\n\n    private val _eventFlow = MutableStateFlow<AudioEvent>(AudioEvent.Idle)\n    val eventFlow: StateFlow<AudioEvent> = _eventFlow.asStateFlow()' \
-e 's/\/\/ TODO: Implement audio toggle/\_eventFlow.value = AudioEvent.ToggleAudio(retroView)/g' > tmp_audio.kt
mv tmp_audio.kt app/src/main/java/com/vinaooo/revenger/viewmodels/AudioViewModel.kt

# Update MenuViewModel
cat app/src/main/java/com/vinaooo/revenger/viewmodels/MenuViewModel.kt | \
sed -e '/private var isDismissingAllMenus/a \
\n    sealed class MenuEvent {\n        object Idle : MenuEvent()\n        data class ShowRetroMenu3(val activity: androidx.fragment.app.FragmentActivity) : MenuEvent()\n        object DismissRetroMenu3 : MenuEvent()\n        object DismissAllMenus : MenuEvent()\n        object ClearControllerInputState : MenuEvent()\n    }\n\n    private val _eventFlow = MutableStateFlow<MenuEvent>(MenuEvent.Idle)\n    val eventFlow: StateFlow<MenuEvent> = _eventFlow.asStateFlow()' \
-e 's/\/\/ TODO: Implement full logic for showing menu/\_eventFlow.value = MenuEvent.ShowRetroMenu3(activity)/g' \
-e 's/\/\/ Por enquanto, delegar para o estado//g' \
-e 's/\/\/ TODO: Implement menu closing logic/\_eventFlow.value = MenuEvent.DismissRetroMenu3/g' \
-e 's/\/\/ TODO: Implement logic to close all menus/\_eventFlow.value = MenuEvent.DismissAllMenus/g' \
-e 's/\/\/ TODO: Implement clearing of controller input state/\_eventFlow.value = MenuEvent.ClearControllerInputState/g' > tmp_menu.kt
mv tmp_menu.kt app/src/main/java/com/vinaooo/revenger/viewmodels/MenuViewModel.kt

# Update InputViewModel
cat app/src/main/java/com/vinaooo/revenger/viewmodels/InputViewModel.kt | \
sed -e '/import com.vinaooo.revenger.input.ControllerInput/a import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow' \
-e '/private var selectStartComboCallback/a \
\n    sealed class InputEvent {\n        object Idle : InputEvent()\n        object HandleSelectStartCombo : InputEvent()\n        data class SetupGamePads(val activity: androidx.fragment.app.FragmentActivity, val leftContainer: android.widget.FrameLayout, val rightContainer: android.widget.FrameLayout) : InputEvent()\n        object ResetComboAlreadyTriggered : InputEvent()\n    }\n\n    private val _eventFlow = MutableStateFlow<InputEvent>(InputEvent.Idle)\n    val eventFlow: StateFlow<InputEvent> = _eventFlow.asStateFlow()' \
-e 's/\/\/ TODO: Implement conditional logic/\_eventFlow.value = InputEvent.HandleSelectStartCombo/g' \
-e 's/\/\/ TODO: Implement gamepad setup/\_eventFlow.value = InputEvent.SetupGamePads(activity, leftContainer, rightContainer)/g' \
-e 's/\/\/ - Create left and right gamepads//g' \
-e 's/\/\/ - Configure layouts//g' \
-e 's/\/\/ - Apply preference settings//g' \
-e 's/\/\/ TODO: Implement resetComboAlreadyTriggered if necessary/\_eventFlow.value = InputEvent.ResetComboAlreadyTriggered/g' \
-e 's/\/\/ controllerInput.resetComboAlreadyTriggered()//g' > tmp_input.kt
mv tmp_input.kt app/src/main/java/com/vinaooo/revenger/viewmodels/InputViewModel.kt

# Update SpeedViewModel
cat app/src/main/java/com/vinaooo/revenger/viewmodels/SpeedViewModel.kt | \
sed -e '/import kotlinx.coroutines.launch/a import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow' \
-e '/private var isFastForwardEnabled: Boolean = false/a \
\n    sealed class SpeedEvent {\n        object Idle : SpeedEvent()\n        data class ToggleFastForward(val retroView: Any?) : SpeedEvent()\n        data class SetGameSpeed(val speed: Int) : SpeedEvent()\n        data class ApplySpeedToController(val controller: SpeedController) : SpeedEvent()\n    }\n\n    private val _eventFlow = MutableStateFlow<SpeedEvent>(SpeedEvent.Idle)\n    val eventFlow: StateFlow<SpeedEvent> = _eventFlow.asStateFlow()' \
-e 's/\/\/ TODO: Implement fast-forward toggle/\_eventFlow.value = SpeedEvent.ToggleFastForward(retroView)/g' \
-e 's/\/\/ TODO: Implement speed configuration in controller/\_eventFlow.value = SpeedEvent.SetGameSpeed(validSpeed)/g' \
-e 's/\/\/ TODO: Apply current speed to controller/\_eventFlow.value = SpeedEvent.ApplySpeedToController(controller)/g' \
-e 's/\/\/ controller.setGameSpeed(currentSpeed)//g' > tmp_speed.kt
mv tmp_speed.kt app/src/main/java/com/vinaooo/revenger/viewmodels/SpeedViewModel.kt

chmod +x update_viewmodels.sh
./update_viewmodels.sh
