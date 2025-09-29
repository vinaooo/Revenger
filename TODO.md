# TODO - Android Library Alignment Issues

## Native Library 16 KB Alignment Issue

**Issue**: The native library `liblibretrodroid.so` from dependency `com.github.swordfish90:libretrodroid:0.12.0` is not 16 KB aligned, only 4 KB aligned.

**Impact**: Apps with 4 KB aligned native libraries may not work correctly on future devices requiring 16 KB alignment.

**Current Status**: 
- Using version 0.12.0 (latest available)
- Applied temporary workaround: disabled `Aligned16KB` lint warning
- No newer version with 16 KB support available yet

**Long-term Solution Needed**:
- Monitor LibretroDroid repository for new releases with 16 KB alignment support
- Update dependency when compatible version becomes available
- Remove lint warning disable once fixed

**Repository to monitor**: https://github.com/Swordfish90/LibretroDroid
**Android documentation**: https://developer.android.com/guide/practices/page-sizes

## Resolved Issues

### ScrollView Nested Issue ✅ RESOLVED
**Issue**: `activity_comprehensive_test.xml` contained nested ScrollViews (vertical ScrollView inside another vertical ScrollView).

**Solution**: Removed the unused layout file `activity_comprehensive_test.xml` as it was not implemented in the app and was causing lint warnings.

**Date resolved**: September 29, 2025

### Resource Reflection Warning ✅ RESOLVED
**Issue**: Android lint warning about discouraged use of `getIdentifier()` for resource reflection in RetroView.kt.

**Solution**: Added `@SuppressLint("DiscouragedApi")` annotation with explanatory comment. Reflection is necessary to maintain project genericness - allows any ROM/emulator combination without recompiling code.

**Date resolved**: September 29, 2025

### Menu Architecture Migration ✅ RESOLVED
**Issue**: Game menu implemented with DialogFragment had limited touch handling - screen edges didn't close the menu reliably.

**Solution**: Migrated from DialogFragment to Fragment architecture with fullscreen overlay for better touch coverage.

**Changes made**:
- Created `GameMenuFullscreenFragment.kt` with proper fullscreen touch handling
- Migrated all menu functionality (reset, save/load, audio, fast forward, exit)
- Added exit confirmation dialog with save prompt
- Applied Material 3 theming consistently
- Removed old `FloatingGameMenu.kt` DialogFragment code
- Cleaned up all references and comments

**Benefits**:
- Reliable touch handling across entire screen
- Better Material 3 integration
- Improved accessibility
- Cleaner codebase with no legacy code

**Date resolved**: September 29, 2025

### RxJava Version Update ✅ RESOLVED
**Issue**: Using outdated RxJava version 3.1.11, newer version 3.1.12 available.

**Solution**: Updated `io.reactivex.rxjava3:rxjava` from 3.1.11 to 3.1.12 in build.gradle.

**Date resolved**: September 29, 2025

### Gradle Version Update ⏸️ DEFERRED
**Issue**: Gradle 9.1.0 is available, currently using 8.14.

**Decision**: Keep Gradle 8.14 for stability reasons.
- Gradle 9.x is a major version with potential breaking changes
- Android Gradle Plugin 8.13.0 may not be compatible with Gradle 9.x
- Current setup (Gradle 8.14 + AGP 8.13.0) is stable and working perfectly
- Will monitor Gradle 9.x maturation and wait for official migration guides

**Current versions**:
- Gradle: 8.14 ✅ (stable)
- Android Gradle Plugin: 8.13.0 ✅ (compatible)
- Kotlin: 2.2.20 ✅ (up-to-date)

**Date noted**: September 29, 2025

### Android 16 Orientation Changes ⏸️ DEFERRED
**Issue**: Android 16 will ignore fixed screen orientations in most cases. Apps should adapt to various orientations, display sizes, and aspect ratios.

**Current status**: 
- App currently uses `android:screenOrientation="landscape"` in AndroidManifest.xml
- This will be ignored on Android 16+ devices
- Affects primarily tablets, foldables, and Chromebooks

**Recommended solution**: Implement device-specific orientation handling
- **Phones**: Keep landscape forced (games need landscape orientation)
- **Tablets**: Allow flexible orientation with adaptive UI
- **Foldables**: Auto-adapt to folding state changes
- **Chromebooks**: Window-based adaptation (ignore orientation)

**Technical approach**:
- Create device type detection utility
- Implement conditional orientation logic
- Design responsive layouts for different orientations
- Preserve game state during orientation changes

**Risks of not implementing**:
- Poor user experience on tablets/foldables
- Potential Play Store rejection for non-adaptive apps
- Ignored orientation settings on future devices

**Decision**: Defer implementation until Android 16 adoption increases. Current landscape-only approach works well for phones (primary target). Monitor adoption rates and implement when necessary.

**Date noted**: September 29, 2025