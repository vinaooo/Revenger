import re

# 1. Update config_manual.md
doc_path = "definitions/config_manual.md"
with open(doc_path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('"cut", "cut2", ou "cut3"', '"upscale1", "upscale2", ou "upscale3"')
with open(doc_path, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. Update RetroView.kt
rv_path = "app/src/main/java/com/vinaooo/revenger/retroview/RetroView.kt"
with open(rv_path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('"cut" -> ShaderConfig.CUT()', '"upscale1" -> ShaderConfig.CUT()')
content = content.replace('"cut2" -> ShaderConfig.CUT2()', '"upscale2" -> ShaderConfig.CUT2()')
content = content.replace('"cut3" -> ShaderConfig.CUT3()', '"upscale3" -> ShaderConfig.CUT3()')

content = content.replace('"cut" -> {', '"upscale1" -> {')
content = content.replace('"cut2" -> {', '"upscale2" -> {')
content = content.replace('"cut3" -> {', '"upscale3" -> {')
with open(rv_path, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. Update ShaderController.kt
sc_path = "app/src/main/java/com/vinaooo/revenger/controllers/ShaderController.kt"
with open(sc_path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('"cut", "cut2", "cut3"', '"upscale1", "upscale2", "upscale3"')
content = content.replace('            "cut" -> "Upscale 1"\n            "cut2" -> "Upscale 2"\n            "cut3" -> "Upscale 3"', '            "upscale1" -> "Upscale 1"\n            "upscale2" -> "Upscale 2"\n            "upscale3" -> "Upscale 3"')
with open(sc_path, 'w', encoding='utf-8') as f:
    f.write(content)

# 4. Update GameActivityViewModel.kt to expose the display name
gvm_path = "app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt"
with open(gvm_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add getShaderDisplayName() right after getShaderState()
if "fun getShaderDisplayName(): String" not in content:
    old_method = """    fun getShaderState(): String {
        return shaderViewModel.getShaderState()
    }"""
    new_method = """    fun getShaderState(): String {
        return shaderViewModel.getShaderState()
    }
    
    fun getShaderDisplayName(): String {
        return shaderViewModel.getCurrentShaderDisplayName()
    }"""
    content = content.replace(old_method, new_method)
    with open(gvm_path, 'w', encoding='utf-8') as f:
        f.write(content)

# 5. Update SettingsMenuFragment.kt to use the display name
smf_path = "app/src/main/java/com/vinaooo/revenger/ui/retromenu3/SettingsMenuFragment.kt"
with open(smf_path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('val currentShader = viewModel.getShaderState()', 'val currentShader = viewModel.getShaderDisplayName()')
with open(smf_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updates to python script finished.")
