"""Audit the port against upstream: which upstream methods are missing in our files?

The 1:1 port copies upstream classes verbatim where possible, but loader glue sometimes
drops whole methods (e.g. TankItem lost its overrideStackedOnOther family). This script
lists, per file, the method names that exist upstream but not in our copy, so the losses
can be reviewed one by one instead of being discovered by users in game.

Usage:
  python tools/audit_missing_methods.py            # summary of worst files
  python tools/audit_missing_methods.py --all      # every file with at least one hit
  python tools/audit_missing_methods.py path/To.java
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
UPSTREAM = ROOT.parent / "ref" / "TinkersConstruct" / "src" / "main" / "java"
PORT = ROOT / "src" / "main" / "java"

# matches "public static ItemStack setTank(", "protected void foo(" etc. and skips keywords
METHOD = re.compile(
    r"^[ \t]*(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public|protected|private)\s+"
    r"(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?"
    r"(?:<[^>]+>\s+)?"
    r"[\w$.<>\[\], ?]+\s+"
    r"(\w+)\s*\(",
    re.MULTILINE,
)

IGNORED_NAMES = {
    # java/lang plumbing that is not worth reporting
    "toString", "equals", "hashCode", "main", "super", "this",
}


def methods(text: str) -> set[str]:
    found: set[str] = set()
    for match in METHOD.finditer(text):
        name = match.group(1)
        if name in IGNORED_NAMES:
            continue
        # skip constructor-looking hits (class name followed by "(")
        if name[:1].isupper() and name not in {"Builder"}:
            continue
        found.add(name)
    return found


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    show_all = "--all" in sys.argv

    if args:
        targets = [ROOT / args[0] if not Path(args[0]).is_absolute() else Path(args[0])]
    else:
        targets = sorted(PORT.rglob("*.java"))

    total_missing = 0
    rows: list[tuple[int, str, list[str]]] = []
    for port_file in targets:
        rel = port_file.relative_to(ROOT / "src" / "main" / "java")
        up_file = UPSTREAM / rel
        if not up_file.exists():
            continue
        up_methods = methods(up_file.read_text(encoding="utf-8", errors="replace"))
        if not up_methods:
            continue
        port_text = port_file.read_text(encoding="utf-8", errors="replace")
        port_methods = methods(port_text)
        missing = sorted(name for name in up_methods - port_methods if name not in port_text)
        if missing:
            total_missing += len(missing)
            rows.append((len(missing), str(rel).replace("\\", "/"), missing))

    rows.sort(key=lambda r: -r[0])
    for count, rel, missing in rows[:40] if not show_all else rows:
        print(f"{count:3d}  {rel}")
        print("      " + ", ".join(missing))
    print(f"\n{len(rows)} file(s), {total_missing} missing method name(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
