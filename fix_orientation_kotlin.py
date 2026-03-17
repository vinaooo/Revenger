import re

# Update DefaultSettingsProfile.kt
profile_file = 'app/src/main/java/com/vinaooo/revenger/models/DefaultSettingsProfile.kt'
with open(profile_file, 'r') as f:
    content = f.read()

content = content.replace('val confOrientation: Int,', 'val confOrientation: String,')
content = content.replace('confOrientation = json.getInt("orientation")', 'confOrientation = json.getString("orientation")')

with open(profile_file, 'w') as f:
    f.write(content)

# Update AppConfig.kt
appconfig_file = 'app/src/main/java/com/vinaooo/revenger/AppConfig.kt'
with open(appconfig_file, 'r') as f:
    content = f.read()

content = content.replace('val orientation: Int = 0,', 'val orientation: String = "landscape",')
content = content.replace('fun getOrientation(): Int = profile?.confOrientation ?: manualConfig.orientation', 'fun getOrientation(): String = profile?.confOrientation ?: manualConfig.orientation')

with open(appconfig_file, 'w') as f:
    f.write(content)

# Update SplashActivity.kt and others that might use it
# Not needed dynamically, AppConfig gives string now, OrientationManager takes string.

print("Updated Models")
