import os

rv_path = "app/src/main/java/com/vinaooo/revenger/retroview/RetroView.kt"
with open(rv_path, 'r', encoding='utf-8') as f:
    content = f.read()

old_apply = """                    "crt" -> ShaderConfig.CRT
                    "lcd" -> ShaderConfig.LCD
                    else -> ShaderConfig.Sharp"""
new_apply = """                    "crt" -> ShaderConfig.CRT
                    "lcd" -> ShaderConfig.LCD
                    "cut" -> ShaderConfig.CUT()
                    "cut2" -> ShaderConfig.CUT2()
                    "cut3" -> ShaderConfig.CUT3()
                    else -> ShaderConfig.Sharp"""
content = content.replace(old_apply, new_apply)

old_get = """            "lcd" -> {
                Log.i("RetroView", "Shader configurado: LCD (efeito de matriz LCD)")
                ShaderConfig.LCD
            }
            else -> {"""
new_get = """            "lcd" -> {
                Log.i("RetroView", "Shader configurado: LCD (efeito de matriz LCD)")
                ShaderConfig.LCD
            }
            "cut" -> {
                Log.i("RetroView", "Shader configurado: CUT (Upsampling Filter 1)")
                ShaderConfig.CUT()
            }
            "cut2" -> {
                Log.i("RetroView", "Shader configurado: CUT2 (Upsampling Filter 2)")
                ShaderConfig.CUT2()
            }
            "cut3" -> {
                Log.i("RetroView", "Shader configurado: CUT3 (Upsampling Filter 3)")
                ShaderConfig.CUT3()
            }
            else -> {"""
content = content.replace(old_get, new_get)

with open(rv_path, 'w', encoding='utf-8') as f:
    f.write(content)


sc_path = "app/src/main/java/com/vinaooo/revenger/controllers/ShaderController.kt"
with open(sc_path, 'r', encoding='utf-8') as f:
    content2 = f.read()

old_array = 'val availableShaders = arrayOf("disabled", "sharp", "crt", "lcd")'
new_array = 'val availableShaders = arrayOf("disabled", "sharp", "crt", "lcd", "cut", "cut2", "cut3")'
content2 = content2.replace(old_array, new_array)

old_display = """            "crt" -> "CRT"
            "lcd" -> "LCD"
            else -> "Unknown\""""
new_display = """            "crt" -> "CRT"
            "lcd" -> "LCD"
            "cut" -> "Upscale 1"
            "cut2" -> "Upscale 2"
            "cut3" -> "Upscale 3"
            else -> "Unknown\""""
content2 = content2.replace(old_display, new_display)

with open(sc_path, 'w', encoding='utf-8') as f:
    f.write(content2)

print("Files updated successfully!")
