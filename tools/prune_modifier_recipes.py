"""Keeps only the modifier recipes the port can execute.

Unsupported upstream shapes (swappable, multilevel) would otherwise log a serializer error per file
on every world load. Re-running this against a fresh extraction is safe.
"""

import json
import pathlib
import sys

SUPPORTED = {"tconstruct:modifier", "tconstruct:incremental_modifier"}

root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".")
kept = 0
removed = 0
for path in root.rglob("*.json"):
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("type") in SUPPORTED:
        kept += 1
    else:
        path.unlink()
        removed += 1
print(f"kept {kept}, removed {removed}")
