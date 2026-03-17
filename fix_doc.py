import re
import glob

def process_md(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    old_text = "- `orientation`: (Integer) 1 = Portrait only, 2 = Landscape only, 3 = Auto."
    new_text = "- `orientation`: (String) \"portrait\" = Portrait only, \"landscape\" = Landscape only, \"auto\" = Any orientation (respects system auto-rotate)."

    if old_text in content:
        content = content.replace(old_text, new_text)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

for f in glob.glob('definitions/*.md') + glob.glob('docs/*.md') + ['README.md']:
    process_md(f)
