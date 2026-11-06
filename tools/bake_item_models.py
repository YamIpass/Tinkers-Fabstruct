"""Rewrites the item models 3.12.1 ships into plain models Fabric can bake.

Tinkers' Construct draws its tool parts and tools with two Forge model loaders:
``tconstruct:material`` (one tinted sprite) and ``tconstruct:tool`` (one tinted sprite
per part). Fabric has no model loader extension in 1.20.1, so those models never bake
and the item renders as the purple/black missing texture.

Vanilla's item generator already builds exactly that shape from ``layer0``..``layerN``
textures, and it assigns tint index N to layer N, which is the same index the tool
stacks its parts in. So the rewrite is just a texture list: ``material`` becomes a
single ``layer0``, ``tool`` becomes one layer per part ordered by part index. The client
colour provider then fills tint index N with material N's colour.

Run it after ``extract_312.py`` (it rewrites files in place):

    python tools/bake_item_models.py [resources_root]

A model whose textures are missing is left untouched, so a broken rewrite stays visible
as an unbaked loader instead of a silently black item.
"""

from __future__ import annotations

import json
import os
import sys

PARENTS = {
    "forge:item/default": "minecraft:item/generated",
    "forge:item/default-tool": "minecraft:item/handheld",
    "forge:item/bucket": "minecraft:item/generated",
    "forge:item/bucket_drip": "minecraft:item/generated",
}


def bake_material(model: dict) -> dict | None:
    textures = model.get("textures") or {}
    texture = textures.get("texture") or textures.get("layer0")
    if not texture:
        return None
    baked = {"parent": "minecraft:item/generated", "textures": {"layer0": texture}}
    if "overrides" in model:
        baked["overrides"] = model["overrides"]
    return baked


def bake_tool(model: dict) -> dict | None:
    textures = model.get("textures") or {}
    parts = model.get("parts") or []
    layers: dict[int, str] = {}
    for part in parts:
        texture = textures.get(part.get("name"))
        if texture:
            layers.setdefault(part.get("index", 0), texture)
    if not layers:
        # Staffs and the flint and brick have a single whole-item sprite instead of parts,
        # named "tool" (their "large_tool" variant only applies to the two-handed form).
        texture = textures.get("tool")
        if texture:
            layers[0] = texture
    if not layers:
        return None
    baked = {
        "parent": "minecraft:item/handheld",
        "textures": {f"layer{index}": texture for index, texture in sorted(layers.items())},
    }
    if "overrides" in model:
        baked["overrides"] = model["overrides"]
    return baked


def rewrite_dump(path: str) -> tuple[str, dict | None]:
    with open(path, encoding="utf-8") as handle:
        model = json.load(handle)
    loader = model.get("loader")
    if loader == "tconstruct:material":
        return "material", bake_material(model)
    if loader == "tconstruct:tool":
        return "tool", bake_tool(model)
    changed = False
    if model.get("parent") in PARENTS:
        model["parent"] = PARENTS[model["parent"]]
        changed = True
    if loader:
        # An unknown loader makes the whole model fail to bake, which is worse than
        # losing the decoration it added.
        model.pop("loader", None)
        changed = True
    return ("parent" if changed else "other"), (model if changed else None)


def main(argv: list[str]) -> int:
    root = argv[1] if len(argv) > 1 else "src/main/resources"
    models = os.path.join(root, "assets", "tconstruct", "models", "item")
    if not os.path.isdir(models):
        print(f"no item models under {models}", file=sys.stderr)
        return 1
    counts: dict[str, int] = {}
    for directory, _, files in os.walk(models):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(directory, name)
            kind, model = rewrite_dump(path)
            counts[kind] = counts.get(kind, 0) + 1
            if model is not None:
                with open(path, "w", encoding="utf-8") as handle:
                    json.dump(model, handle, indent=2)
                    handle.write("\n")
    print("baked item models:", dict(sorted(counts.items())))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
