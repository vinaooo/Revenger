#!/bin/bash

# Test script to open submenu and immediately return to check back stack listener
echo "Starting submenu back stack test..."

# Start the app
adb shell am start -n com.vinaooo.revenger/.views.GameActivity

# Wait for app to load
sleep 3

# Navigate to Progress menu (index 2)
echo "Navigating to Progress menu..."
adb shell input keyevent 20  # DOWN
sleep 0.5
adb shell input keyevent 20  # DOWN
sleep 0.5

# Confirm to open Progress submenu
echo "Opening Progress submenu..."
adb shell input keyevent 23  # ENTER/DPAD_CENTER
sleep 2

# Immediately press B to go back
echo "Pressing B to return from submenu..."
adb shell input keyevent 4   # BACK
sleep 2

# Check logs for back stack listener
echo "Checking logs for back stack listener..."
adb logcat -d | grep -i "Back stack empty" | tail -5

echo "Test completed."