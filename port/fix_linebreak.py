import re, sys, pathlib

root = pathlib.Path(sys.argv[1])
apply = "--apply" in sys.argv
changed = 0
for path in root.rglob("*.java"):
    if path.name in ("Icons.java", "GuiUtil.java"):
        continue
    text = path.read_text(encoding="utf-8")
    orig = text
    out = []
    lines = text.split("\n")
    i = 0
    while i < len(lines):
        line = lines[i]
        m = re.match(r"^(\s*)([A-Za-z_]\w*)\.linebreak = true;\s*$", line)
        if m:
            indent, var = m.group(1), m.group(2)
            # find the next line that adds this var to a list
            j = i + 1
            while j < len(lines) and not re.search(r"\b\w+\.add\(\s*" + re.escape(var) + r"\s*\)\s*;", lines[j]):
                j += 1
            if j < len(lines):
                out.append(lines[j])
                lst = re.search(r"\b(\w+)\.add\(", lines[j]).group(1)
                out.append(f"{indent}{lst}.add(TextComponentData.LINEBREAK);")
                i = j + 1
                continue
        m2 = re.match(r"^(\s*)(\w+)\.add\(new (TextComponentData|TextData)\((.*)\)\.linebreak\(true\)\);\s*$", line)
        if m2:
            indent, lst, cls, args = m2.groups()
            out.append(f"{indent}{lst}.add(new {cls}({args}));")
            out.append(f"{indent}{lst}.add({cls}.LINEBREAK);")
            i += 1
            continue
        out.append(line)
        i += 1
    text = "\n".join(out)
    if text != orig:
        changed += 1
        rel = path.relative_to(root)
        if apply:
            path.write_text(text, encoding="utf-8")
        print(f"{rel}")
print(f"-- files changed: {changed}")
