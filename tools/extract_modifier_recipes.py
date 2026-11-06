"""Copies the modifier recipes the port can execute out of a Tinkers' Construct jar.

Upstream ships several recipe shapes for modifiers; this port implements the plain one and the
incremental one (redstone-style accumulation). Everything else is skipped so the recipe manager does
not log a serializer error for every file on each world load.

Usage: python extract_modifier_recipes.py <mod.jar> <resources-dir>
"""

import json
import pathlib
import sys
import zipfile

SUPPORTED = {"tconstruct:modifier", "tconstruct:incremental_modifier"}
PREFIX = "data/tconstruct/recipes/tools/modifiers/"


def main() -> int:
    jar_path = pathlib.Path(sys.argv[1])
    resources = pathlib.Path(sys.argv[2])
    copied = 0
    skipped = 0
    with zipfile.ZipFile(jar_path) as jar:
        for name in jar.namelist():
            if not name.startswith(PREFIX) or not name.endswith(".json"):
                continue
            if "/salvage/" in name:
                continue
            data = json.loads(jar.read(name).decode("utf-8"))
            if data.get("type") not in SUPPORTED:
                skipped += 1
                continue
            target = resources / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
            copied += 1
    print(f"copied {copied}, skipped {skipped}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
