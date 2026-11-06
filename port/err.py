#!/usr/bin/env python3
"""Report helper for the library/client port: parse /tmp/lc/errors.txt.

  err.py summary            per-file error counts inside library/client (sorted)
  err.py show <rel>         full error blocks for one slice-relative file,
                            e.g. show model/tools/ToolModel.java
  err.py msgs               cluster of distinct error messages in the slice
  err.py all                every slice error line (compact)
"""
import os
import re
import sys

ERR = os.environ.get(
    "ERR_FILE",
    os.path.join(os.environ.get("TEMP", r"C:\Users\YamIpass\AppData\Local\Temp"),
                 "lc", "errors.txt"))
CLIENT = "library\\client\\"
HDR = re.compile(r"^(.*?\.java):(\d+): error: (.*)$")


def parse():
    out = []
    if not os.path.exists(ERR):
        sys.exit("no errors.txt; run: bash port/client_cp.sh compile")
    with open(ERR, encoding="utf-8", errors="replace") as fh:
        cur = None
        for line in fh:
            line = line.rstrip("\n")
            m = HDR.match(line)
            if m:
                cur = {"file": m.group(1), "line": int(m.group(2)),
                       "msg": m.group(3), "ctx": []}
                out.append(cur)
            elif cur is not None:
                cur["ctx"].append(line)
    return out


def rel(path):
    i = path.find(CLIENT)
    if i < 0:
        return None
    return path[i + len(CLIENT):].replace("\\", "/")


def main():
    errs = parse()
    slice_errs = [e for e in errs if CLIENT in e["file"]]
    mode = sys.argv[1] if len(sys.argv) > 1 else "summary"
    if mode == "summary":
        counts = {}
        for e in slice_errs:
            counts[rel(e["file"])] = counts.get(rel(e["file"]), 0) + 1
        print(f"slice errors: {len(slice_errs)} in {len(counts)} files")
        for name, n in sorted(counts.items(), key=lambda kv: -kv[1]):
            print(f"{n:5d}  {name}")
        print(f"total tree errors: {len(errs)}")
    elif mode == "show":
        want = sys.argv[2].replace("\\", "/")
        hits = [e for e in slice_errs if rel(e["file"]) == want]
        if not hits:
            print(f"no errors in {want}")
        for e in hits:
            print(f"{want}:{e['line']}: {e['msg']}")
            for c in e["ctx"][:3]:
                print("      " + c)
    elif mode == "msgs":
        c = {}
        for e in slice_errs:
            c[e["msg"]] = c.get(e["msg"], 0) + 1
        for m, n in sorted(c.items(), key=lambda kv: -kv[1])[:60]:
            print(f"{n:5d}  {m[:150]}")
    elif mode == "all":
        for e in slice_errs:
            print(f"{rel(e['file'])}:{e['line']}: {e['msg']}")


if __name__ == "__main__":
    main()
