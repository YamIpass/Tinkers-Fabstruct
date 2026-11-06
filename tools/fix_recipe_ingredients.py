#!/usr/bin/env python3
"""Fix custom-ingredient JSON in the ported data.

Tinkers 3.12.1 (Forge) marks its custom ingredients with a plain "type" key. Fabric's
CustomIngredient API reads "fabric:type", and vanilla's RecipeManager reads the recipe-root "type".
Two mechanical rewrites are needed after extraction:

1. recipe root: "fabric:type" -> "type"   (root is a recipe type, never an ingredient)
2. nested ingredients: "type" -> "fabric:type" for the six tconstruct ingredient types
3. an array ingredient that contains a custom ingredient is wrapped as fabric:any

Usage: fix_recipe_ingredients.py [--apply]
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "src" / "main" / "resources" / "data"

CUSTOM = {
    "tconstruct:material",
    "tconstruct:material_value",
    "tconstruct:no_container",
    "tconstruct:instrument",
    "tconstruct:block_tag",
    "tconstruct:tool_hook",
}

INGREDIENT_KEYS = {"ingredient", "ingredients", "cast", "pattern", "pattern_item", "key"}


def strip_comments(text):
    out = []
    i, n = 0, len(text)
    in_str = False
    while i < n:
        c = text[i]
        if in_str:
            out.append(c)
            if c == "\\" and i + 1 < n:
                out.append(text[i + 1])
                i += 2
                continue
            if c == '"':
                in_str = False
            i += 1
            continue
        if c == '"':
            in_str = True
            out.append(c)
            i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                i += 1
            i += 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def is_custom_ingredient(value):
    return isinstance(value, dict) and value.get("type") in CUSTOM and "fabric:type" not in value


def fix_ingredient(value):
    """Normalize one ingredient slot (dict, list or array ingredient), returns (new_value, changed)."""
    if isinstance(value, dict):
        if is_custom_ingredient(value):
            value["fabric:type"] = value.pop("type")
            return value, True
        changed = False
        for key in list(value.keys()):
            if key in INGREDIENT_KEYS:
                value[key], c = fix_ingredient(value[key])
                changed |= c
        return value, changed
    if isinstance(value, list):
        items = []
        changed = False
        for v in value:
            nv, c = fix_ingredient(v)
            items.append(nv)
            changed |= c
        # an array ingredient containing a custom ingredient must become a fabric:any ingredient
        if any(isinstance(v, dict) and ("fabric:type" in v or v.get("type") in CUSTOM) for v in items):
            return {"fabric:type": "fabric:any", "ingredients": items}, True
        return items, changed
    return value, False


def fix_node(node):
    changed = False
    if isinstance(node, dict):
        for key, child in list(node.items()):
            if key in INGREDIENT_KEYS:
                if key == "key" and isinstance(child, dict):
                    for k, v in list(child.items()):
                        child[k], c = fix_ingredient(v)
                        changed |= c
                else:
                    node[key], c = fix_ingredient(child)
                    changed |= c
            else:
                node[key], c = fix_node(child)
                changed |= c
    elif isinstance(node, list):
        for i, v in enumerate(node):
            node[i], c = fix_node(v)
            changed |= c
    return node, changed


def main():
    apply = "--apply" in sys.argv
    files = sorted(DATA.rglob("*.json"))
    root_fixed = nested_fixed = 0
    changed_files = []
    for path in files:
        text = path.read_text(encoding="utf-8")
        if "fabric:type" not in text and "tconstruct:" not in text:
            continue
        try:
            data = json.loads(strip_comments(text))
        except json.JSONDecodeError:
            continue
        if not isinstance(data, dict):
            continue
        changed = False
        # 1. recipe root
        if "fabric:type" in data and "type" not in data:
            data["type"] = data.pop("fabric:type")
            changed = True
            root_fixed += 1
        # 2/3. nested ingredients
        data, c = fix_node(data)
        if c:
            changed = True
            nested_fixed += 1
        if changed:
            changed_files.append(path)
            if apply:
                path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"recipe roots restored: {root_fixed}")
    print(f"files with nested ingredient fixes: {nested_fixed}")
    print(f"changed files: {len(changed_files)}")
    for p in changed_files[:10]:
        print("  ", p.relative_to(ROOT))
    if not apply:
        print("dry run; pass --apply to write")


if __name__ == "__main__":
    main()
