import os
import re

def update_file(path, event_name, event_params, methods_to_replace):
    with open(path, 'r') as f:
        content = f.read()

    # Add imports
    if 'import kotlinx.coroutines.flow.MutableStateFlow' not in content:
        content = content.replace('import androidx.lifecycle.AndroidViewModel\n',
                                  'import androidx.lifecycle.AndroidViewModel\nimport kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow\n')
    else:
        if 'import kotlinx.coroutines.flow.asStateFlow' not in content:
            content = content.replace('import kotlinx.coroutines.flow.StateFlow\n', 'import kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow\n')

    # Find where to insert StateFlow
    events = []
    events.append(f"sealed class {event_name} {{")
    events.append(f"    object Idle : {event_name}()")
    for param in event_params:
        if isinstance(param, tuple) and param[1]:
            events.append(f"    data class {param[0]}({param[1]}) : {event_name}()")
        else:
            p_name = param[0] if isinstance(param, tuple) else param
            events.append(f"    object {p_name} : {event_name}()")
    events.append("}")
    
    event_str = "\n    " + "\n    ".join(events) + f"\n\n    private val _eventFlow = MutableStateFlow<{event_name}>({event_name}.Idle)\n    val eventFlow: StateFlow<{event_name}> = _eventFlow.asStateFlow()\n"
    
    class_def_regex = r"(class \w+\(.*\) : AndroidViewModel\([^)]+\)\s*\{)(.*?)"
    
    match = re.search(class_def_regex, content, re.DOTALL)
    if match:
        insertion_point = match.group(1)
        content = content.replace(insertion_point, insertion_point + "\n" + event_str, 1)

    for m in methods_to_replace:
        find_re = m['find']
        replace_with = m['replace']
        content = re.sub(find_re, replace_with, content, flags=re.DOTALL)

    with open(path, 'w') as f:
        f.write(content)

# GameStateViewModel
update_file(
    'app/src/main/java/com/vinaooo/revenger/viewmodels/GameStateViewModel.kt',
    'GameStateEvent',
    ['ResetGame', ('SaveState', 'val slot: Int'), ('LoadState', 'val slot: Int'), ('CheckSaveState', 'val slot: Int'), ('SetGameSpeed', 'val speed: Int')],
    [
        {
            'find': r'// TODO: Implement game reset logic\n\s*// - Save temporary state if needed\n\s*// - Reset the emulator\n\s*// - Restore settings',
            'replace': '_eventFlow.value = GameStateEvent.ResetGame'
        },
        {
            'find': r'// TODO: Implement save state',
            'replace': '_eventFlow.value = GameStateEvent.SaveState(slot)'
        },
        {
            'find': r'// TODO: Implement load state',
            'replace': '_eventFlow.value = GameStateEvent.LoadState(slot)'
        },
        {
            'find': r'// TODO: Check if save state exists',
            'replace': '_eventFlow.value = GameStateEvent.CheckSaveState(slot)'
        },
        {
            'find': r'// TODO: Implement game speed configuration\n\s*// Validate range \(normally 1-2\)\n\s*// Apply to emulator',
            'replace': '_eventFlow.value = GameStateEvent.SetGameSpeed(speed)'
        }
    ]
)

# AudioViewModel
update_file(
    'app/src/main/java/com/vinaooo/revenger/viewmodels/AudioViewModel.kt',
    'AudioEvent',
    [('ToggleAudio', 'val retroView: Any?')],
    [
        {
            'find': r'// TODO: Implement audio toggle',
            'replace': '_eventFlow.value = AudioEvent.ToggleAudio(retroView)'
        }
    ]
)

# MenuViewModel
update_file(
    'app/src/main/java/com/vinaooo/revenger/viewmodels/MenuViewModel.kt',
    'MenuEvent',
    [('ShowRetroMenu3', 'val activity: androidx.fragment.app.FragmentActivity'), 'DismissRetroMenu3', 'DismissAllMenus', 'ClearControllerInputState'],
    [
        {
            'find': r'// TODO: Implement full logic for showing menu\n\s*// Por enquanto, delegar para o estado',
            'replace': '_eventFlow.value = MenuEvent.ShowRetroMenu3(activity)'
        },
        {
            'find': r'// TODO: Implement menu closing logic',
            'replace': '_eventFlow.value = MenuEvent.DismissRetroMenu3'
        },
        {
            'find': r'// TODO: Implement logic to close all menus',
            'replace': '_eventFlow.value = MenuEvent.DismissAllMenus'
        },
        {
            'find': r'// TODO: Implement clearing of controller input state',
            'replace': '_eventFlow.value = MenuEvent.ClearControllerInputState'
        }
    ]
)

# InputViewModel
update_file(
    'app/src/main/java/com/vinaooo/revenger/viewmodels/InputViewModel.kt',
    'InputEvent',
    ['HandleSelectStartCombo', ('SetupGamePads', 'val activity: androidx.fragment.app.FragmentActivity, val leftContainer: android.widget.FrameLayout, val rightContainer: android.widget.FrameLayout'), 'ResetComboAlreadyTriggered'],
    [
        {
            'find': r'// TODO: Implement conditional logic',
            'replace': '_eventFlow.value = InputEvent.HandleSelectStartCombo\n            true'
        },
        {
            'find': r'// TODO: Implement gamepad setup\n\s*// - Create left and right gamepads\n\s*// - Configure layouts\n\s*// - Apply preference settings',
            'replace': '_eventFlow.value = InputEvent.SetupGamePads(activity, leftContainer, rightContainer)'
        },
        {
            'find': r'// TODO: Implement resetComboAlreadyTriggered if necessary\n\s*// controllerInput\.resetComboAlreadyTriggered\(\)',
            'replace': '_eventFlow.value = InputEvent.ResetComboAlreadyTriggered'
        },
        {
            'find': r'controllerInput\.shouldHandleSelectStartCombo = \{\n\s*true\n\s*\} _eventFlow\.value = InputEvent\.HandleSelectStartCombo',
            'replace': 'controllerInput.shouldHandleSelectStartCombo = {\n            _eventFlow.value = InputEvent.HandleSelectStartCombo\n            true\n        }'
        }
    ]
)

# SpeedViewModel
update_file(
    'app/src/main/java/com/vinaooo/revenger/viewmodels/SpeedViewModel.kt',
    'SpeedEvent',
    [('ToggleFastForward', 'val retroView: Any?'), ('SetGameSpeed', 'val speed: Int'), ('ApplySpeedToController', 'val controller: SpeedController')],
    [
        {
            'find': r'// TODO: Implement fast-forward toggle',
            'replace': '_eventFlow.value = SpeedEvent.ToggleFastForward(retroView)'
        },
        {
            'find': r'// TODO: Implement speed configuration in controller',
            'replace': '_eventFlow.value = SpeedEvent.SetGameSpeed(validSpeed)' # validSpeed used here
        },
        {
            'find': r'// TODO: Apply current speed to controller\n\s*// controller\.setGameSpeed\(currentSpeed\)',
            'replace': '_eventFlow.value = SpeedEvent.ApplySpeedToController(controller)'
        }
    ]
)

