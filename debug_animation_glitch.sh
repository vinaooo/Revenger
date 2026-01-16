#!/bin/bash
echo "=== DEBUG MENU ANIMATION GLITCH - VISUAL EFFECTS ==="
echo "Foco: AnimationOptimizer logs para verificar se animações estão sendo executadas"
echo "Pressione Ctrl+C para parar"
echo ""
adb logcat | grep -E "(\[BATCH_ANIM\]|\[ANIMATE_IN\]|\[ANIMATE_OUT\]|AnimationOptimizer)"

