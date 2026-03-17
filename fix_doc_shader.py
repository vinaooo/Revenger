import re

doc_path = "definitions/config_manual.md"
with open(doc_path, 'r', encoding='utf-8') as f:
    content = f.read()

old_doc = """- `shader`: (String) Define o estilo de renderização da tela. Valores aceitos: `"disabled"`, `"sharp"`, `"crt"`, ou `"lcd"`."""
new_doc = """- `shader`: (String) Define o estilo de renderização da tela. Valores aceitos: `"disabled"`, `"sharp"`, `"crt"`, `"lcd"`, `"cut"`, `"cut2"`, ou `"cut3"`."""

content = content.replace(old_doc, new_doc)

with open(doc_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Doc updated!")
