#!/bin/bash

# Phase 9.5: Comprehensive Testing & Validation Suite
# Final validation for Revenger Emulator project

echo "🔍 Phase 9.5: Starting Final Validation & Testing Suite..."
echo "================================================="

# Initialize test results
TESTS_PASSED=0
TESTS_FAILED=0
TEST_LOG="validation_results_$(date +%Y%m%d_%H%M%S).log"

# Function to log test results
log_test() {
    echo "[$1] $2" | tee -a "$TEST_LOG"
    if [ "$1" = "PASS" ]; then
        ((TESTS_PASSED++))
    else
        ((TESTS_FAILED++))
    fi
}

echo "Starting validation at $(date)" > "$TEST_LOG"

# Test 1: Build System Validation
echo "🔧 Testing Build System Performance..."
echo "-----------------------------------"

# Test build-fast.sh
echo "Testing build-fast.sh..."
BUILD_START=$(date +%s.%3N)
if ./build-fast.sh > /dev/null 2>&1; then
    BUILD_END=$(date +%s.%3N)
    BUILD_TIME=$(echo "scale=2; $BUILD_END - $BUILD_START" | bc)
    if (( $(echo "$BUILD_TIME < 5.0" | bc -l) )); then
        log_test "PASS" "build-fast.sh completed in ${BUILD_TIME}s (target: <5s)"
    else
        log_test "FAIL" "build-fast.sh took ${BUILD_TIME}s (exceeds 5s target)"
    fi
else
    log_test "FAIL" "build-fast.sh failed to execute"
fi

# Test build-sdk36.sh
echo "Testing build-sdk36.sh..."
BUILD_START=$(date +%s.%3N)
if ./build-sdk36.sh > /dev/null 2>&1; then
    BUILD_END=$(date +%s.%3N)
    BUILD_TIME=$(echo "scale=2; $BUILD_END - $BUILD_START" | bc)
    if (( $(echo "$BUILD_TIME < 10.0" | bc -l) )); then
        log_test "PASS" "build-sdk36.sh completed in ${BUILD_TIME}s (target: <10s)"
    else
        log_test "FAIL" "build-sdk36.sh took ${BUILD_TIME}s (exceeds 10s target)"
    fi
else
    log_test "FAIL" "build-sdk36.sh failed to execute"
fi

# Test 2: APK Validation
echo "📦 Testing APK Generation & Properties..."
echo "---------------------------------------"

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    log_test "PASS" "APK file generated successfully"
    
    # Check APK size
    APK_SIZE_MB=$(du -m "$APK_PATH" | cut -f1)
    if [ "$APK_SIZE_MB" -lt 50 ]; then
        log_test "PASS" "APK size (${APK_SIZE_MB}MB) within acceptable range (<50MB)"
    else
        log_test "FAIL" "APK size (${APK_SIZE_MB}MB) exceeds 50MB limit"
    fi
    
    # Validate APK structure using unzip (fallback for aapt)
    if unzip -t "$APK_PATH" > /dev/null 2>&1; then
        log_test "PASS" "APK structure is valid (zip test passed)"
        
        # Extract manifest and check SDK versions using alternative method
        if unzip -p "$APK_PATH" AndroidManifest.xml > /tmp/manifest.xml 2>/dev/null; then
            # This is a simplified check - in production you'd parse the binary XML
            log_test "PASS" "AndroidManifest.xml extracted successfully"
        else
            log_test "WARN" "Could not extract AndroidManifest.xml for SDK validation"
        fi
    else
        log_test "FAIL" "APK structure validation failed"
    fi
else
    log_test "FAIL" "APK file not found at $APK_PATH"
fi

# Test 3: Code Quality & Structure
echo "📝 Testing Code Quality & Structure..."
echo "------------------------------------"

# Check for Phase 9 implementations
if [ -f "app/src/main/java/com/vinaooo/revenger/utils/AndroidCompatibility.kt" ]; then
    log_test "PASS" "AndroidCompatibility framework present"
else
    log_test "FAIL" "AndroidCompatibility framework missing"
fi

if [ -f "app/src/main/java/com/vinaooo/revenger/ui/theme/DynamicThemeManager.kt" ]; then
    log_test "PASS" "DynamicThemeManager (SDK 36) present"
else
    log_test "FAIL" "DynamicThemeManager missing"
fi

if [ -f "app/src/main/java/com/vinaooo/revenger/privacy/EnhancedPrivacyManager.kt" ]; then
    log_test "PASS" "EnhancedPrivacyManager (SDK 36) present"
else
    log_test "FAIL" "EnhancedPrivacyManager missing"
fi

if [ -f "app/src/main/java/com/vinaooo/revenger/performance/AdvancedPerformanceProfiler.kt" ]; then
    log_test "PASS" "AdvancedPerformanceProfiler (SDK 36) present"
else
    log_test "FAIL" "AdvancedPerformanceProfiler missing"
fi

# Check for RxJava migration completion
RX_BRIDGE_COUNT=$(find app/src/main -name "*.kt" -exec grep -l "RxJavaBridge\|Observable\.<" {} \; 2>/dev/null | wc -l)
if [ "$RX_BRIDGE_COUNT" -eq 0 ]; then
    log_test "PASS" "RxJava Bridge migration completed (no legacy code found)"
else
    log_test "WARN" "Found $RX_BRIDGE_COUNT files with potential RxJava Bridge code"
fi

# Check for Flow usage
FLOW_COUNT=$(find app/src/main -name "*.kt" -exec grep -l "Flow\|collect\|lifecycleScope" {} \; 2>/dev/null | wc -l)
if [ "$FLOW_COUNT" -gt 0 ]; then
    log_test "PASS" "Kotlin Flow implementation present ($FLOW_COUNT files)"
else
    log_test "FAIL" "No Kotlin Flow implementation found"
fi

# Test 4: Documentation Coverage
echo "📚 Testing Documentation Coverage..."
echo "----------------------------------"

REQUIRED_DOCS=(
    "docs/phase-9-1-radialgamepad-update.md"
    "docs/phase-9-2-flow-migration.md" 
    "docs/phase-9-3-gradle-optimization.md"
    "docs/phase-9-4-sdk36-features.md"
    "docs/backward-compatibility.md"
)

for doc in "${REQUIRED_DOCS[@]}"; do
    if [ -f "$doc" ]; then
        log_test "PASS" "Documentation present: $doc"
    else
        log_test "FAIL" "Missing documentation: $doc"
    fi
done

# Test 5: Gradle Configuration Validation
echo "⚙️ Testing Gradle Configuration..."
echo "--------------------------------"

# Check for performance optimizations in gradle.properties
if [ -f "gradle.properties" ]; then
    if grep -q "org.gradle.caching=true" gradle.properties; then
        log_test "PASS" "Build cache enabled in gradle.properties"
    else
        log_test "FAIL" "Build cache not enabled"
    fi
    
    if grep -q "org.gradle.parallel=true" gradle.properties; then
        log_test "PASS" "Parallel execution enabled"
    else
        log_test "FAIL" "Parallel execution not enabled"
    fi
else
    log_test "FAIL" "gradle.properties file missing"
fi

# Test 6: Dependency Validation
echo "📦 Testing Dependencies..."
echo "-------------------------"

# Check for RadialGamePad 2.0.0
if grep -q "radialgamepad:2.0.0" app/build.gradle; then
    log_test "PASS" "RadialGamePad 2.0.0 dependency present"
else
    log_test "FAIL" "RadialGamePad 2.0.0 dependency missing"
fi

# Check for Material Design (SDK 36 feature)
if grep -q "material:1.12.0" app/build.gradle; then
    log_test "PASS" "Material Design Components present"
else
    log_test "FAIL" "Material Design Components missing"
fi

# Final Results
echo ""
echo "🏁 VALIDATION COMPLETE"
echo "======================"
echo "📊 Test Results Summary:"
echo "   ✅ Tests Passed: $TESTS_PASSED"
echo "   ❌ Tests Failed: $TESTS_FAILED"
echo "   📝 Full log: $TEST_LOG"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
SUCCESS_RATE=$(echo "scale=1; $TESTS_PASSED * 100 / $TOTAL_TESTS" | bc)

echo "   📈 Success Rate: ${SUCCESS_RATE}%"

if [ "$TESTS_FAILED" -eq 0 ]; then
    echo ""
    echo "🎉 ALL TESTS PASSED! Project ready for production."
    echo "✅ Phase 9.5 validation successful!"
    exit 0
else
    echo ""
    echo "⚠️  Some tests failed. Review the log for details."
    echo "❌ Phase 9.5 validation needs attention."
    exit 1
fi
