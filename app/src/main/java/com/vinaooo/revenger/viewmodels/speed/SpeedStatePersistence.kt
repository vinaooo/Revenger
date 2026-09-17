package com.vinaooo.revenger.viewmodels.speed

import com.vinaooo.revenger.repositories.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Loads and saves `SpeedViewModel`'s game-speed / fast-forward preferences. Split out purely to
 * keep `SpeedViewModel` under the project's function-count threshold; all four methods were
 * private there and are only ever called from within `SpeedViewModel` itself, so they stay a
 * plain injected instance (no interface delegation) here.
 */
class SpeedStatePersistence(
        private val preferencesRepository: PreferencesRepository,
        private val scope: CoroutineScope
) {
    // SharedPreferences.getInt()/getBoolean() throw ClassCastException if a value stored under
    // that key isn't the requested type (e.g. a stale value from a preferences-format change).
    fun loadSpeedState(onLoaded: (Int) -> Unit, onFailure: (ClassCastException) -> Unit) {
        scope.launch {
            try {
                onLoaded(preferencesRepository.getGameSpeedSync())
            } catch (e: ClassCastException) {
                onFailure(e)
            }
        }
    }

    fun loadFastForwardState(onLoaded: (Boolean) -> Unit, onFailure: (ClassCastException) -> Unit) {
        scope.launch {
            try {
                onLoaded(preferencesRepository.getFastForwardEnabledSync())
            } catch (e: ClassCastException) {
                onFailure(e)
            }
        }
    }

    // SharedPreferences.Editor.putX()/apply() don't declare or realistically throw on the
    // standard Android implementation; kept broad via the escape hatch as a defensive net for
    // this fire-and-forget write, since there's no narrower reachable type to name.
    fun saveSpeedState(speed: Int, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                preferencesRepository.setGameSpeed(speed)
            } catch (expectedPreferencesWriteFailure: Exception) {
                onFailure(expectedPreferencesWriteFailure)
            }
        }
    }

    fun saveFastForwardState(enabled: Boolean, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                preferencesRepository.setFastForwardEnabled(enabled)
            } catch (expectedPreferencesWriteFailure: Exception) {
                onFailure(expectedPreferencesWriteFailure)
            }
        }
    }
}
