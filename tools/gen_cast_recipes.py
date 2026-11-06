"""Writes the crafting recipes that turn a pattern into a gold cast.

Upstream makes gold casts in the casting table, which this port does not have yet. A shapeless
recipe — pattern plus a gold ingot — gives the same "buy the shape once, reuse it forever" result
in a crafting grid, and the cast is returned by the part builder like a bucket, so one cast serves
any number of parts.

Run after ``extract_312.py``; the recipes live outside the extracted tree, so they survive a
re-extract, but rerunning is harmless:

    python tools/gen_cast_recipes.py
"""

from __future__ import annotations

import json
import os
import sys

# Shapes upstream ships a gold cast for, matching TinkerToolParts.CASTABLE.
CASTABLE = [
    "pick_head", "hammer_head", "small_axe_head", "broad_axe_head", "adze_head",
    "small_blade", "broad_blade", "large_plate", "tool_handle", "tough_handle",
    "tool_binding", "tough_binding", "bow_limb", "bow_grip", "maille", "repair_kit",
]


def recipe(part: str) -> dict:
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [
            {"item": "tconstruct:pattern"},
            {"tag": "forge:ingots/gold"},
        ],
        "result": {"item": f"tconstruct:{part}_cast"},
    }


def main(argv: list[str]) -> int:
    root = argv[1] if len(argv) > 1 else "src/main/resources"
    out = os.path.join(root, "data", "tconstruct", "recipes", "tools", "parts", "cast")
    os.makedirs(out, exist_ok=True)
    for part in CASTABLE:
        path = os.path.join(out, f"{part}_cast.json")
        with open(path, "w", encoding="utf-8") as handle:
            json.dump(recipe(part), handle, indent=2)
            handle.write("\n")
    print(f"wrote {len(CASTABLE)} cast recipes to {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
