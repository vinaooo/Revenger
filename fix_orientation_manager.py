import re

file_path = 'app/src/main/java/com/vinaooo/revenger/utils/OrientationManager.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Replace method signatures
content = content.replace("fun applyConfigOrientation(activity: Activity, configOrientation: Int)", "fun applyConfigOrientation(activity: Activity, configOrientation: String)")
content = content.replace("fun forceConfigurationBeforeSetContent(activity: Activity, configOrientation: Int)", "fun forceConfigurationBeforeSetContent(activity: Activity, configOrientation: String)")

# Replace when statement blocks in applyConfigOrientation
old_apply_when = """        val orientation = when (configOrientation) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT // Sempre portrait
            2 -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE // Sempre landscape
            3 -> {
                // If config is "any orientation", respect OS preference
                if (accelerometerRotationEnabled) {
                    // Auto-rotate enabled → allow free rotation based on sensors
                    ActivityInfo.SCREEN_ORIENTATION_USER
                } else {
                    // Auto-rotate disabled → delegate completely to the system
                    // UNSPECIFIED allows the system manual button to work
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }"""

new_apply_when = """        val orientation = when (configOrientation.lowercase()) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT // Sempre portrait
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE // Sempre landscape
            "auto" -> {
                // If config is "any orientation", respect OS preference
                if (accelerometerRotationEnabled) {
                    // Auto-rotate enabled → allow free rotation based on sensors
                    ActivityInfo.SCREEN_ORIENTATION_USER
                } else {
                    // Auto-rotate disabled → delegate completely to the system
                    // UNSPECIFIED allows the system manual button to work
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }"""
            
content = content.replace(old_apply_when, new_apply_when)

# Replace when in forceConfigurationBeforeSetContent
old_force_check = """        // For mode 3 (any orientation), do not force anything - let Android decide
        if (configOrientation == 3) {"""
new_force_check = """        // For auto mode, do not force anything - let Android decide
        if (configOrientation.lowercase() == "auto") {"""
        
content = content.replace(old_force_check, new_force_check)

old_force_when = """        // Determine the desired orientation
        val desiredOrientation = when (configOrientation) {
            1 -> Configuration.ORIENTATION_PORTRAIT
            2 -> Configuration.ORIENTATION_LANDSCAPE"""
            
new_force_when = """        // Determine the desired orientation
        val desiredOrientation = when (configOrientation.lowercase()) {
            "portrait" -> Configuration.ORIENTATION_PORTRAIT
            "landscape" -> Configuration.ORIENTATION_LANDSCAPE"""

content = content.replace(old_force_when, new_force_when)

with open(file_path, 'w') as f:
    f.write(content)

print("Updated OrientationManager")
