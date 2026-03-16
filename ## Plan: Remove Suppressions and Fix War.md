## Plan: Remove Suppressions and Fix Warnings

I will remove all hardcoded suppression annotations (`@Suppress`) across the codebase and fix the underlying issues causing the warnings, adhering to modern Android APIs while maintaining backward compatibility.

**Steps**
1. **Fix Unused Parameters in ViewModels & Utils:**
   - [app/src/main/java/com/vinaooo/revenger/viewmodels/InputViewModel.kt](app/src/main/java/com/vinaooo/revenger/viewmodels/InputViewModel.kt)
   - [app/src/main/java/com/vinaooo/revenger/viewmodels/AudioViewModel.kt](app/src/main/java/com/vinaooo/revenger/viewmodels/AudioViewModel.kt)
   - [app/src/main/java/com/vinaooo/revenger/viewmodels/SpeedViewModel.kt](app/src/main/java/com/vinaooo/revenger/viewmodels/SpeedViewModel.kt)
   - [app/src/main/java/com/vinaooo/revenger/viewmodels/GameStateViewModel.kt](app/src/main/java/com/vinaooo/revenger/viewmodels/GameStateViewModel.kt)
   - [app/src/main/java/com/vinaooo/revenger/utils/LibRetroDownloader.kt](app/src/main/java/com/vinaooo/revenger/utils/LibRetroDownloader.kt)
   - *Action:* Remove `@Suppress("UNUSED_PARAMETER")`. I will remove the unused parameters from the method signatures. If these methods are bound to UI events (e.g., XML Data Binding passing `View`), I will update the callers to use parameterless lambdas or keep the parameter if strictly required by an interface, though typical clean code suggests removing them if unused.

2. **Fix Deprecated Audio APIs:**
   - [app/src/main/java/com/vinaooo/revenger/managers/AudioRoutingManager.kt](app/src/main/java/com/vinaooo/revenger/managers/AudioRoutingManager.kt)
   - *Action:* Remove `@Suppress("DEPRECATION")`. Replace deprecated checks (like `isWiredHeadsetOn` or legacy focus requests) with the modern `AudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)` and `AudioDeviceInfo` APIs.

3. **Fix Deprecated Display APIs:**
   - [app/src/main/java/com/vinaooo/revenger/utils/OrientationManager.kt](app/src/main/java/com/vinaooo/revenger/utils/OrientationManager.kt)
   - *Action:* Remove `@Suppress("DEPRECATION")`. Replace legacy `Display` metrics and rotation calls with `WindowManager.currentWindowMetrics` (API 30+).

4. **Fix Deprecated Activity Transitions:**
   - [app/src/main/java/com/vinaooo/revenger/views/SplashActivity.kt](app/src/main/java/com/vinaooo/revenger/views/SplashActivity.kt)
   - *Action:* Remove `@Suppress("DEPRECATION")` for `overridePendingTransition`. Use a conditional check for API 34+ to use `overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)`, falling back to `overridePendingTransition` for older Android versions (down to minSdk 30).

**Verification**
- Run `./gradlew lintDebug` to ensure no new warnings or errors are introduced.
- Run `./gradlew assembleDebug` to confirm the project compiles successfully.

**Decisions**
- I will use `Build.VERSION.SDK_INT` checks when fixing deprecations to ensure compatibility between `minSdk 30` and `targetSdk 36`.