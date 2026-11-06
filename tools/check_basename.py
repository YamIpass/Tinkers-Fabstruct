import os, re, sys
from collections import defaultdict

WS = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace"
PROJ = os.path.join(WS, "tconstruct-fabric")
THEIRS = os.path.join(WS, "ref", "Hephaestus-1.20.1", "src", "main", "java", "slimeknights", "tconstruct")
BASE = os.path.join(WS, "ref", "TConstruct-3.6.4", "src", "main", "java", "slimeknights", "tconstruct")

# index by basename for both trees
def index(root):
    idx = defaultdict(list)
    for dirpath, _, names in os.walk(root):
        for n in names:
            if n.endswith(".java"):
                idx[n].append(os.path.relpath(os.path.join(dirpath, n), root).replace("\\", "/"))
    return idx

ti = index(THEIRS)
bi = index(BASE)

batch = os.path.join(PROJ, "build", "batches", "tools.txt")
files = []
for line in open(batch, encoding="utf-8"):
    m = re.match(r"^(tools\\\S+)\s+\((\d+)\)", line.strip())
    if m:
        files.append((m.group(1).replace("\\", "/") + ".java", int(m.group(2))))

for rel, nerr in files:
    name = os.path.basename(rel)
    if os.path.exists(os.path.join(THEIRS, rel)):
        continue
    print(f"{nerr:3} {rel}   | heph: {ti.get(name)} | base364: {bi.get(name)}")
