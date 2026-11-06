"""Print errors whose symbol/message contains a substring, for slice files only.

Usage: py.sh port/locate.py <substring>   (matches the symbol or message text)
"""
import os
import re
import sys

ERR = os.environ.get("ERR_FILE", os.path.join(os.environ.get("TEMP", "/tmp"), "lc", "errors.txt"))
needle = sys.argv[1] if len(sys.argv) > 1 else ""
text = open(ERR, encoding="utf-8", errors="replace").read()
blocks = re.split(r"\n(?=\S+\.java:\d+: )", text)

out = []
for b in blocks:
    if needle not in b:
        continue
    head = b.split("\n")[0]
    if "library\\client\\" not in head:
        continue
    loc = re.search(r"library\\client\\(.*)$", head).group(1)
    body = b.split("\n\n")[0].split("\n")
    src = ""
    if len(body) > 1:
        src = body[1].strip()
    msg = head.split("error: ", 1)[-1] if "error: " in head else ""
    out.append("%s | %s" % (loc, src))

for line in out:
    print(line)
print("-- total:", len(out))
