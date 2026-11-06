#!/usr/bin/env python3
"""Check every import in the library/client slice against the classpath index."""
import os
import re
import sys
from collections import Counter, defaultdict

TMP = os.environ.get("TEMP", r"C:\Users\YamIpass\AppData\Local\Temp")
IDX = os.path.join(TMP, "lc", "cpindex.txt")
ROOT = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace\tconstruct-fabric\src\main\java\slimeknights\tconstruct\library\client"
SRC = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace\tconstruct-fabric\src\main\java"

IMP = re.compile(r"^import\s+(static\s+)?([\w.$]+);$", re.M)


def index():
    names = set()
    with open(IDX, encoding="utf-8") as fh:
        for line in fh:
            if "\t" in line:
                names.add(line.split("\t", 1)[0])
    return names


def candidates(fqcn, names):
    """Resolve fqcn, allowing nested-class dotted form."""
    if fqcn in names:
        return [fqcn]
    # try turning last dots into $
    parts = fqcn.split(".")
    out = []
    for i in range(len(parts) - 1, 0, -1):
        cand = ".".join(parts[:i]) + "$" + "$".join(parts[i:])
        if cand in names:
            out.append(cand)
    return out


def local_exists(fqcn):
    path = os.path.join(SRC, fqcn.replace(".", os.sep) + ".java")
    if os.path.exists(path):
        return True
    # nested
    parts = fqcn.split(".")
    for i in range(len(parts) - 1, 0, -1):
        p = os.path.join(SRC, ".".join(parts[:i]).replace(".", os.sep) + ".java")
        if os.path.exists(p):
            return True
    return False


def main():
    names = index()
    missing = defaultdict(list)
    for dirpath, _dirs, files in os.walk(ROOT):
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(dirpath, name)
            rel = os.path.relpath(path, ROOT).replace("\\", "/")
            text = open(path, encoding="utf-8").read()
            for m in IMP.finditer(text):
                static, fqcn = m.group(1), m.group(2)
                if fqcn.startswith("java.") or fqcn.startswith("javax."):
                    continue
                if not candidates(fqcn, names) and not local_exists(fqcn):
                    missing[fqcn].append(rel)
    for fqcn, files in sorted(missing.items(), key=lambda kv: (-len(kv[1]), kv[0])):
        print(f"{len(files):3d}  {fqcn}   [{', '.join(files[:4])}]")
    print(f"-- unresolved imports: {len(missing)} distinct")


if __name__ == "__main__":
    main()
