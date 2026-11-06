import os, re, subprocess, sys

WS = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace"
PROJ = os.path.join(WS, "tconstruct-fabric")
SRC = os.path.join(PROJ, "src", "main", "java", "slimeknights", "tconstruct")
BASE = os.path.join(WS, "ref", "TConstruct-3.6.4", "src", "main", "java", "slimeknights", "tconstruct")
THEIRS = os.path.join(WS, "ref", "Hephaestus-1.20.1", "src", "main", "java", "slimeknights", "tconstruct")

batch = os.path.join(PROJ, "build", "batches", "tools.txt")
files = []
for line in open(batch, encoding="utf-8"):
    m = re.match(r"^(tools\\\S+)\s+\((\d+)\)", line.strip())
    if m:
        files.append((m.group(1).replace("\\", "/") + ".java", int(m.group(2))))

outdir = os.path.join(PROJ, "build", "merge3")
os.makedirs(outdir, exist_ok=True)

rows = []
for rel, nerr in files:
    ours = os.path.join(SRC, rel)
    base = os.path.join(BASE, rel)
    theirs = os.path.join(THEIRS, rel)
    has_b = os.path.exists(base)
    has_t = os.path.exists(theirs)
    status = ""
    conflicts = 0
    if not has_t:
        status = "NO_THEIRS"
    elif not has_b:
        status = "NO_BASE"
    else:
        r = subprocess.run(["diff3", "-m", "-E", ours, base, theirs],
                           capture_output=True, text=True, encoding="utf-8", errors="replace")
        merged = r.stdout
        conflicts = merged.count("\n<<<<<<<")
        status = "CLEAN" if conflicts == 0 else "CONFLICT"
        dest = os.path.join(outdir, rel)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        open(dest, "w", encoding="utf-8", newline="\n").write(merged)
    rows.append((rel, nerr, status, conflicts, has_b, has_t))

rows.sort(key=lambda r: (r[2] != "CLEAN", -r[1]))
from collections import Counter
c = Counter(r[2] for r in rows)
print("total", len(rows), dict(c))
print("clean errors covered:", sum(r[1] for r in rows if r[2] == "CLEAN"), "of", sum(r[1] for r in rows))
print()
for rel, nerr, status, conflicts, hb, ht in rows:
    print(f"{status:10} {conflicts:3} conflicts  {nerr:3} err  {rel}")
