"""Expand the `texture` field of Mantle retextured shaped recipes.

Forge Mantle's `mantle:crafting_shaped_retextured` recipe writes `"texture": "w"`, the letter of the pattern key whose
item provides the block texture. The Fabric Mantle build reads that field with `Ingredient.fromJson`, which needs a
real ingredient object, so the key letter is replaced by the matching `key` entry. Semantics are unchanged: the texture
is still picked from the first item matching that ingredient.

Usage: fix_retextured_texture.py [--apply]
"""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src" / "main" / "resources" / "data"


def main(apply: bool) -> None:
    changed = 0
    broken = []
    for path in sorted(DATA.rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        if "crafting_shaped_retextured" not in text:
            continue
        data = json.loads(text)
        texture = data.get("texture")
        key = data.get("key")
        if not isinstance(texture, str) or not isinstance(key, dict):
            continue
        ingredient = key.get(texture)
        if ingredient is None:
            broken.append(str(path))
            continue
        data["texture"] = ingredient
        changed += 1
        if apply:
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"retextured recipes fixed: {changed}")
    if broken:
        print("missing key for texture:", len(broken))
        for path in broken[:5]:
            print("  ", path)
    if not apply:
        print("dry run, pass --apply to write")


if __name__ == "__main__":
    main("--apply" in sys.argv)
