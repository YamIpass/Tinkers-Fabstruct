"""Drops Mantle fluid transfer data files that reference content from mods we do not ship.

Mantle 1.11 reads its load conditions from the ``fabric:load_conditions`` key, while upstream 3.12.1 data files use
plain ``conditions`` with Forge condition types. Those are therefore ignored and every transfer file is loaded, which
fails loudly for the ones describing Twilight Forest or Create items.

For a port without those mods the correct outcome is the same as Forge's: the file is skipped, so dropping it keeps
behaviour identical while removing the load error.
"""
import json, sys
from pathlib import Path

ALLOWED = {"minecraft", "tconstruct", "c", "forge", "porting_lib", "mantle", "fabric"}
ROOT = Path("src/main/resources/data/tconstruct/mantle/fluid_transfer")


def namespaces(node, out):
    if isinstance(node, dict):
        for key, value in node.items():
            if key in {"item", "fluid"} and isinstance(value, str):
                out.add(value.split(":")[0] if ":" in value else "minecraft")
            else:
                namespaces(value, out)
    elif isinstance(node, list):
        for entry in node:
            namespaces(entry, out)


def main(apply):
    dropped = []
    for path in sorted(ROOT.glob("*.json")):
        found = set()
        namespaces(json.loads(path.read_text(encoding="utf-8")), found)
        if found - ALLOWED:
            dropped.append((path.name, sorted(found - ALLOWED)))
            if apply:
                path.unlink()
    for name, ns in dropped:
        print("drop", name, ns)
    print(f"checked {len(list(ROOT.glob('*.json')))} files, dropped {len(dropped)}")


if __name__ == "__main__":
    main("--apply" in sys.argv)
