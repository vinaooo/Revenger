#!/bin/bash
# Fast Development Build Script
# Phase 9.3: Gradle Advanced Performance Optimizations

echo "🚀 Starting optimized build..."

# Set optimal JVM arguments for build performance
export GRADLE_OPTS="-Xmx8192m -XX:+UseG1GC -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError"

# Use Gradle build cache and parallel execution
./gradlew assembleDebug \
  --build-cache \
  --parallel \
  --daemon \
  --offline 2>/dev/null || ./gradlew assembleDebug \
  --build-cache \
  --parallel \
  --daemon

echo "✅ Build completed!"

# Show APK info
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    APK_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "📦 APK size: $APK_SIZE"
    echo "📁 APK location: app/build/outputs/apk/debug/app-debug.apk"
fi
