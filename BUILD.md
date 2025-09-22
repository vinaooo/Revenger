# 🚀 Revenger - Build Instructions

## 🏗️ **Building the Project**

Use standard Gradle commands for building:

### **Debug Build**
```bash
./gradlew assembleDebug
```

### **Release Build**
```bash
./gradlew assembleRelease
```

### **Clean Build**
```bash
./gradlew clean assembleDebug
```

### **Install on Device**
```bash
./gradlew installDebug
```

## 📱 **APK Location**
After building, the APK will be available at:
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

## 🛠️ **Manual Installation**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📋 **Expected Build Results**
- **APK Size:** ~36MB (includes LibretroDroid cores)
- **Build Time:** ~3-5 seconds for incremental builds
- **Supported Architectures:** arm64-v8a, armeabi-v7a, x86, x86_64

---

**Note:** Always use standard Gradle commands for reliability and consistency.
