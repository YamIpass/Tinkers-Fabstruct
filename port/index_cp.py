#!/usr/bin/env python3
"""Build a fqcn -> jar index over every jar on the javac classpath."""
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
TMP = os.environ.get("TEMP", r"C:\Users\YamIpass\AppData\Local\Temp")
CP_LINES = os.path.join(TMP, "lc", "cp_lines.txt")
OUT = os.path.join(TMP, "lc", "cpindex.txt")

def main():
    jars = [l.strip() for l in open(CP_LINES, encoding="utf-8") if l.strip()]
    seen = {}
    rows = []
    for jar in jars:
        if not os.path.exists(jar):
            print("missing: " + jar, file=sys.stderr)
            continue
        try:
            with zipfile.ZipFile(jar) as zf:
                for name in zf.namelist():
                    if not name.endswith(".class"):
                        continue
                    if name.startswith("META-INF/"):
                        continue
                    fqcn = name[:-6].replace("/", ".")
                    if fqcn in seen:
                        continue
                    seen[fqcn] = jar
                    rows.append(fqcn + "\t" + os.path.basename(jar))
        except zipfile.BadZipFile:
            print("bad zip: " + jar, file=sys.stderr)
    rows.sort()
    with open(OUT, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(rows) + "\n")
    print(f"{len(rows)} classes from {len(jars)} jars -> {OUT}")

if __name__ == "__main__":
    main()
