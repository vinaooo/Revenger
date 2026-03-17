import json
import glob

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        modified = False
        
        def migrate_obj(obj):
            child_mod = False
            if isinstance(obj, dict):
                if 'orientation' in obj and isinstance(obj['orientation'], int):
                    val = obj['orientation']
                    if val == 1:
                        obj['orientation'] = "portrait"
                        child_mod = True
                    elif val == 2:
                        obj['orientation'] = "landscape"
                        child_mod = True
                    elif val == 3:
                        obj['orientation'] = "auto"
                        child_mod = True
                
                for k, v in obj.items():
                    if isinstance(v, (dict, list)):
                        if migrate_obj(v):
                            child_mod = True
                return child_mod
                
            elif isinstance(obj, list):
                for item in obj:
                    if migrate_obj(item):
                        child_mod = True
                return child_mod
            return False

        if migrate_obj(data):
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=4)
            print(f"Migrated orientation in: {filepath}")
            
    except Exception as e:
        print(f"Error processing {filepath}: {e}")

search_patterns = ['app/src/main/assets/**/*.json', 'config_backup/*.json']
for pattern in search_patterns:
    for filepath in glob.glob(pattern, recursive=True):
        process_file(filepath)
