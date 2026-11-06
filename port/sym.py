"""Aggregate javac 'cannot find symbol' detail (symbol: ...) for library/client errors."""
import os
import re
import sys
from collections import Counter

ERR = os.environ.get("ERR_FILE", os.path.join(os.environ.get("TEMP", "/tmp"), "lc", "errors.txt"))
SLICE = "library\\client\\"

entries = []  # (relfile, line, kind, detail)
with open(ERR, encoding="utf-8", errors="replace") as f:
    lines = f.read().split("\n")

cur = None
for i, line in enumerate(lines):
    m = re.match(r"^(.*\.java):(\d+): (error: )?(.*)$", line)
    if m and SLICE in m.group(1):
        rel = m.group(1).split(SLICE)[1]
        cur = {"file": rel, "line": m.group(2), "msg": m.group(4), "symbol": "", "loc": ""}
        entries.append(cur)
        continue
    if cur is not None and re.match(r"^\s*(symbol|location):", line):
        key = line.strip().split(":", 1)[0]
        val = line.strip().split(":", 1)[1].strip()
        if key == "symbol":
            cur["symbol"] = val
        else:
            cur["loc"] = val

mode = sys.argv[1] if len(sys.argv) > 1 else "symbols"
if mode == "symbols":
    c = Counter(e["symbol"] for e in entries if e["symbol"])
    for text, n in c.most_common(70):
        print(f"{n:5d}  {text}")
elif mode == "msgs":
    c = Counter(e["msg"] for e in entries)
    for text, n in c.most_common(50):
        print(f"{n:5d}  {text}")
elif mode == "files":
    c = Counter(e["file"] for e in entries)
    for text, n in c.most_common(200):
        print(f"{n:5d}  {text}")
elif mode == "for":
    target = sys.argv[2]
    for e in entries:
        if e["file"].endswith(target):
            print(f'{e["line"]}: {e["msg"]}  [{e["symbol"]}]  {e["loc"]}')
