"""Removes members whose signature references Mantle's removed HTML book API.
Strict: only starts at a declaration line (indented) whose signature mentions Html/toHTML."""
import sys
from pathlib import Path

MARKERS = ("HtmlSerializable", "HtmlElement", "HtmlGroup", "HTMLUtils", "toHTML(", "makeTitleHTML(")


def is_decl_start(line: str) -> bool:
    s = line.rstrip("\n")
    if not s.startswith(("  ", "\t")):
        return False
    if "{" in s:
        head = s.split("{", 1)[0]
    else:
        head = s
    if "(" not in head:
        return False
    return any(tok in head for tok in MARKERS)


def strip(path: Path, dry: bool) -> int:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    out, removed, i, n = [], 0, 0, len(lines)
    while i < n:
        line = lines[i]
        if is_decl_start(line):
            start = i
            if "{" in line or (i + 1 < n and "{" in lines[i + 1] and lines[i].rstrip().endswith(")")):
                depth, j, started = 0, i, False
                while j < n:
                    depth += lines[j].count("{") - lines[j].count("}")
                    if "{" in lines[j]:
                        started = True
                    if started and depth <= 0:
                        break
                    j += 1
                end = j
            else:
                end = start
            while out and out[-1].strip() in ("@Override", "@Deprecated", ""):
                if out[-1].strip() == "" and out and len(out) > 1 and out[-2].strip() != "":
                    break
                out.pop()
            removed += 1
            print(f"  {path.name}: remove lines {start + 1}-{end + 1}")
            i = end + 1
            continue
        out.append(line)
        i += 1
    if not dry:
        path.write_text("".join(out), encoding="utf-8")
    return removed


if __name__ == "__main__":
    dry = "--apply" not in sys.argv
    root = Path([a for a in sys.argv[1:] if not a.startswith("--")][0])
    total = 0
    for p in sorted(root.rglob("*.java")):
        total += strip(p, dry)
    print(f"total removed {total}{' (dry run)' if dry else ''}")
