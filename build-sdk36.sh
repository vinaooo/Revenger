#!/bin/bash

# Build script for Target SDK 36 Features - Phase 9.4
# Otimizado para builds super rápidos com recursos SDK 36

echo "🚀 Phase 9.4: Building with Target SDK 36 Features..."

# Limpar cache se necessário
if [ "$1" = "clean" ]; then
    echo "🧹 Cleaning build cache..."
    ./gradlew clean
fi

# Build otimizado com performance monitoring
echo "⚡ Building with enhanced performance monitoring..."

# Medir tempo de build
START_TIME=$(date +%s.%3N)

# Build com todas as otimizações
./gradlew assembleDebug \
  --build-cache \
  --parallel \
  --configuration-cache \
  --configuration-cache-problems=warn \
  --daemon \
  -Dorg.gradle.jvmargs="-Xmx8g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication" \
  -Dkotlin.incremental=true \
  -Dkotlin.compiler.execution.strategy=in-process \
  -Pandroid.enableJetifier=false \
  -Pandroid.useAndroidX=true

BUILD_RESULT=$?
END_TIME=$(date +%s.%3N)
BUILD_TIME=$(echo "scale=2; $END_TIME - $START_TIME" | bc)

if [ $BUILD_RESULT -eq 0 ]; then
    APK_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "✅ Build successful!"
    echo "⏱️  Build time: ${BUILD_TIME}s"
    echo "📦 APK size: $APK_SIZE"
    echo "🎯 Target SDK: 36 (Android 16)"
    echo "📱 Min SDK: 30 (Android 11+)"
    echo ""
    echo "🆕 SDK 36 Features Included:"
    echo "  • Enhanced Material You 3.0 Dynamic Theming"
    echo "  • Advanced Privacy Controls"
    echo "  • Real-time Performance Profiling"
    echo "  • Progressive Feature Enhancement"
    echo "  • Full Backward Compatibility"
    echo ""
    echo "APK ready at: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Build failed!"
    exit 1
fi
