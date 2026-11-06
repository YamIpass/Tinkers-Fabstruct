import os, re, sys, zipfile

WS = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace"
PROJ = os.path.join(WS, "tconstruct-fabric")
OUT = os.path.join(PROJ, "build", "merge3")
LIBS = os.path.join(PROJ, "libs")
SRCJ = os.path.join(PROJ, "src", "main", "java")

IMPORT_RE = re.compile(r"^\s*import ([a-zA-Z0-9_.]+);\s*$")
FABRIC_PREFIX = ("io.github.fabricators_of_create", "net.fabricmc", "me.alphamode")

# ---- known classes: our source tree + jars
known = set()
for dirpath, _, names in os.walk(SRCJ):
    for n in names:
        if n.endswith(".java"):
            known.add(os.path.relpath(os.path.join(dirpath, n), SRCJ)[:-5].replace("\\", "/"))
jarclasses = set()
for j in os.listdir(LIBS):
    if j.endswith(".jar"):
        try:
            with zipfile.ZipFile(os.path.join(LIBS, j)) as z:
                for n in z.namelist():
                    if n.endswith(".class"):
                        jarclasses.add(n[:-6])
        except Exception as e:
            print("jar fail", j, e)

def exists(fq):
    parts = fq.split(".")
    for k in range(len(parts), 1, -1):
        outer = "/".join(parts[:k])
        rest = "$".join(parts[k:])
        key = outer + ("$" + rest if rest else "")
        if key in jarclasses or key in known:
            return True
        if outer in known:
            return True
    return False

def resolve_import_conflicts(text):
    lines = text.split("\n")
    out = []
    extras = []
    i = 0
    n = 0
    while i < len(lines):
        ln = lines[i]
        if ln.startswith("<<<<<<< "):
            j = i + 1
            ours = []
            while j < len(lines) and not lines[j].startswith("======="):
                ours.append(lines[j]); j += 1
            theirs = []
            j += 1
            while j < len(lines) and not lines[j].startswith(">>>>>>> "):
                theirs.append(lines[j]); j += 1
            alll = [x for x in ours + theirs if x.strip()]
            if all(IMPORT_RE.match(x) for x in alll):
                # keep ours' imports (3.12.1 tree), add Fabric-only imports from theirs
                for x in ours:
                    if x.strip() and "net.minecraftforge" not in x:
                        out.append(x)
                for x in theirs:
                    m = IMPORT_RE.match(x)
                    if m and m.group(1).startswith(FABRIC_PREFIX):
                        extras.append(x.strip())
                n += 1
                i = j + 1
                continue
            # unresolved conflict: emit verbatim
            out.extend(lines[i:j + 1])
            i = j + 1
            continue
        out.append(ln)
        i += 1
    return "\n".join(out), extras, n

def insert_extras(text, extras):
    if not extras:
        return text
    seen = set(re.findall(r"^\s*import ([a-zA-Z0-9_.]+);", text, re.M))
    new = []
    for e in extras:
        fq = IMPORT_RE.match(e).group(1)
        if fq not in seen:
            seen.add(fq)
            new.append(e)
    if not new:
        return text
    lines = text.split("\n")
    # insert after last import line
    last = max(i for i, l in enumerate(lines) if l.startswith("import "))
    lines[last + 1:last + 1] = new
    return "\n".join(lines)

report = []
for dirpath, _, names in os.walk(OUT):
    for nm in names:
        if not nm.endswith(".java"):
            continue
        p = os.path.join(dirpath, nm)
        rel = os.path.relpath(p, OUT).replace("\\", "/")
        text = open(p, encoding="utf-8").read()
        newtext, extras, resolved = resolve_import_conflicts(text)
        newtext = insert_extras(newtext, extras)
        if newtext != text:
            open(p, "w", encoding="utf-8", newline="\n").write(newtext)
        confs = newtext.count("\n<<<<<<< ")
        # validate slimeknights imports
        bad = []
        for fq in re.findall(r"^\s*import (slimeknights\.[a-zA-Z0-9_.]+);", newtext, re.M):
            if not exists(fq):
                bad.append(fq)
        report.append((rel, resolved, confs, bad))

report.sort()
for rel, resolved, confs, bad in report:
    flags = []
    if confs:
        flags.append(f"{confs} conflict(s) left")
    if bad:
        flags.append("MISSING: " + ", ".join(bad))
    if flags:
        print(f"{rel}: {'; '.join(flags)}")
print()
print("files with 0 conflicts & clean imports:", sum(1 for r in report if r[2] == 0 and not r[3]))
print("total import-only conflicts resolved:", sum(r[1] for r in report))
