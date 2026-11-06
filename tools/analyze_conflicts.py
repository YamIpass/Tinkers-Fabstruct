import os, re, sys
from collections import defaultdict

WS = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace"
PROJ = os.path.join(WS, "tconstruct-fabric")
OUT = os.path.join(PROJ, "build", "merge3")

CONF_RE = re.compile(r"^<{7} |^={7}$|^>{7} ")

def parse_conflicts(text):
    """return list of (start_idx, end_idx, ours_lines, theirs_lines) over line list"""
    lines = text.split("\n")
    out = []
    i = 0
    while i < len(lines):
        if lines[i].startswith("<<<<<<< "):
            j = i + 1
            ours = []
            while j < len(lines) and not lines[j].startswith("=======") :
                ours.append(lines[j]); j += 1
            theirs = []
            j += 1
            while j < len(lines) and not lines[j].startswith(">>>>>>> "):
                theirs.append(lines[j]); j += 1
            out.append((i, j, ours, theirs))
            i = j
        i += 1
    return out, lines

IMPORT_ONLY = re.compile(r"^\s*import [a-zA-Z0-9_.]+;\s*$")
FABRIC_PREFIX = ("io.github.fabricators_of_create", "net.fabricmc", "me.alphamode", "net.fabricmc.fabric")

tot = defaultdict(int)
for dirpath, _, names in os.walk(OUT):
    for n in names:
        if not n.endswith(".java"):
            continue
        p = os.path.join(dirpath, n)
        rel = os.path.relpath(p, OUT).replace("\\", "/")
        text = open(p, encoding="utf-8").read()
        confs, lines = parse_conflicts(text)
        imp_only = 0
        other = 0
        for s, e, ours, theirs in confs:
            ok = True
            for ln in ours + theirs:
                if ln.strip() == "":
                    continue
                if not IMPORT_ONLY.match(ln):
                    ok = False
                    break
            if ok:
                imp_only += 1
            else:
                other += 1
        if confs:
            print(f"{rel}: {len(confs)} conflicts, {imp_only} import-only, {other} other")
        tot["imp"] += imp_only
        tot["other"] += other
print(dict(tot))
