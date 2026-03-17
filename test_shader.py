import json

path = "app/src/main/assets/default_settings.json"
with open(path, 'r', encoding='utf-8') as f:
    data = json.load(f)

for item in data:
    if item.get("platform_id") == "sms":
        item["shader"] = "upscale1"

with open(path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4)
print("Updated SMS to upscale1 in default_settings.json")
