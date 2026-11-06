"""Strips HTML-book-only members (methods/initializers whose body references Mantle's removed html API)
from the book classes. Brace-matching removal, keeps everything else verbatim."""
import re
import sys
from pathlib import Path

HTML_TOKENS = ("HtmlSerializable", "HtmlElement", "HtmlGroup", "HTMLUtils")

def strip(path: Path) -> int:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    out = []
    i = 0
    removed = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        # candidate declarations: method (has '(' and '{' possibly later) or field initializer
        if "{" in line or (line.rstrip().endswith(")") and i + 1 < n and "{" in lines[i + 1]):
            # find the block start and match braces
            start = i
            depth = 0
            started = False
            j = i
            body = []
            while j < n:
                cur = lines[j]
                body.append(cur)
                depth += cur.count("{") - cur.count("}")
                if "{" in cur:
                    started = True
                if started and depth <= 0:
                    break
                j += 1
            block = "".join(body)
            if started and any(tok in block for tok in HTML_TOKENS):
                # also swallow a preceding @Override line
                while out and out[-1].strip() in ("@Override", "@Deprecated"):
                    out.pop()
                removed += 1
                i = j + 1
                continue
        out.append(line)
        i += 1
    path.write_text("".join(out), encoding="utf-8")
    return removed

if __name__ == "__main__":
    root = Path(sys.argv[1])
    total = 0
    for p in sorted(root.rglob("*.java")):
        r = strip(p)
        if r:
            print(f"{p.name}: removed {r}")
            total += r
    print(f"total removed {total}")
