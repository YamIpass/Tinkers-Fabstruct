#!/usr/bin/env python3
"""Normalize Forge-only recipe JSON into shapes the Fabric port can load.

- forge compound ingredients     -> fabric:all / fabric:any (fabric:difference folded to its base)
- forge:conditional recipes      -> flattened; passing branches written as their own files
- recipes referencing mods we do not ship are dropped (compat recipes)

Slot lists ("ingredients" of a shapeless recipe, "inputs" of an alloy) are lists of separate
ingredient slots, so a custom ingredient in them must NOT be wrapped into fabric:any; only real
alternative lists ("cast", "pattern", "ingredient", "key" values) get the wrapper.

Usage: normalize_forge_data.py [--apply]
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "src" / "main" / "resources" / "data"

# "milk" is the fluid namespace of the bundled milk-lib mod, which our port uses for the skeleton melting recipes
KNOWN_NS = {"minecraft", "tconstruct", "c", "forge", "milk"}
KNOWN_MODS = {"minecraft", "tconstruct", "fabric", "fabric-api", "mantle", "porting_lib", "portinglib"}

COMPOUND = {
    "forge:and": "fabric:all",
    "forge:intersection": "fabric:all",
    "forge:or": "fabric:any",
    "forge:union": "fabric:any",
}

# keys whose value is a list of separate ingredient slots
SLOT_LIST_KEYS = {"ingredients", "inputs"}

NS_RE = re.compile(r'^([a-z0-9_.-]+):[a-z0-9_./-]+$')


def collect_value_strings(obj, found):
    if isinstance(obj, dict):
        for key, value in obj.items():
            if key in ("item", "fluid", "result") and isinstance(value, str):
                found.add(value)
            collect_value_strings(value, found)
    elif isinstance(obj, list):
        for item in obj:
            collect_value_strings(item, found)


def unknown_namespaces(obj):
    found = set()
    collect_value_strings(obj, found)
    bad = set()
    for value in found:
        match = NS_RE.match(value)
        if match and match.group(1) not in KNOWN_NS:
            bad.add(match.group(1))
    return bad


def eval_condition(cond):
    if not isinstance(cond, dict):
        return True
    kind = cond.get("type", "")
    if kind in ("forge:true", "mantle:true"):
        return True
    if kind in ("forge:false", "mantle:false"):
        return False
    if kind.endswith("mod_loaded"):
        return cond.get("modid") in KNOWN_MODS
    if kind == "forge:item_exists":
        return str(cond.get("item", "")).split(":")[0] in KNOWN_NS
    if kind in ("forge:and", "mantle:and"):
        return all(eval_condition(c) for c in cond.get("values", cond.get("children", [])))
    if kind in ("forge:or", "mantle:or"):
        return any(eval_condition(c) for c in cond.get("values", cond.get("children", [])))
    if kind in ("forge:not", "mantle:not"):
        return not eval_condition(cond.get("value", cond.get("child")))
    # config driven recipes: the shipped defaults enable these features
    if kind == "tconstruct:config":
        return True
    if kind.endswith("tag_filled") or kind.endswith("tag_combination_filled"):
        return True
    if kind.endswith("tag_empty"):
        return False
    return False


def eval_conditions(conditions):
    return all(eval_condition(c) for c in conditions)


def convert_ingredients(node, in_slot_list=False):
    """Replace forge compound ingredients with fabric ones, recursively."""
    changed = False
    if isinstance(node, dict):
        kind = node.get("type")
        if kind in COMPOUND:
            children = node.get("children", node.get("values", []))
            children = [convert_ingredients(c)[0] for c in children]
            return {"fabric:type": COMPOUND[kind], "ingredients": children}, True
        if kind == "forge:difference":
            base = node.get("base", {"item": "minecraft:air"})
            return convert_ingredients(base)[0], True
        for key, value in list(node.items()):
            child = convert_ingredients(value, key in SLOT_LIST_KEYS)
            node[key], c = child
            changed |= c
        return node, changed
    if isinstance(node, list):
        items = []
        for value in node:
            nv, c = convert_ingredients(value)
            items.append(nv)
            changed |= c
        # an alternative list holding a custom ingredient must become a fabric:any ingredient
        if not in_slot_list and any(
            isinstance(v, dict) and ("fabric:type" in v or v.get("type") in COMPOUND) for v in items
        ):
            return {"fabric:type": "fabric:any", "ingredients": items}, True
        return items, changed
    return node, changed


def strip_conditions(recipe):
    if isinstance(recipe, dict) and "conditions" in recipe:
        recipe = dict(recipe)
        recipe.pop("conditions")
        return recipe, True
    return recipe, False


def flatten(obj, keep, drop_cond, drop_ns):
    """Unwrap forge:conditional wrappers anywhere in the tree. Mutates counters."""
    if isinstance(obj, dict):
        if obj.get("type") == "forge:conditional":
            pass  # handled by the caller
        for key, value in list(obj.items()):
            obj[key] = flatten(value, keep, drop_cond, drop_ns)
        if obj.get("type") == "forge:conditional":
            produced = []
            for entry in obj.get("recipes", []):
                if not isinstance(entry, dict):
                    continue
                recipe = entry.get("recipe", entry)
                if not eval_conditions(entry.get("conditions", [])):
                    drop_cond.append(None)
                    continue
                recipe, _ = strip_conditions(recipe)
                if unknown_namespaces(recipe):
                    drop_ns.append(None)
                    continue
                produced.append(recipe)
            if not produced:
                return None
            if len(produced) == 1:
                return produced[0]
            keep.append(produced)
            return produced
        return obj
    if isinstance(obj, list):
        for i, value in enumerate(obj):
            obj[i] = flatten(value, keep, drop_cond, drop_ns)
        return obj
    return obj


def repair_slot_lists(node):
    """Undo the earlier mistake of wrapping a slot list ("ingredients" of a shapeless recipe) into
    a single fabric:any ingredient: those keys must stay a list of separate slots."""
    changed = False
    if isinstance(node, dict):
        for key, value in list(node.items()):
            if key in SLOT_LIST_KEYS and isinstance(value, dict) and value.get("fabric:type") == "fabric:any":
                node[key] = value.get("ingredients", [])
                changed = True
            node[key], c = repair_slot_lists(node[key])
            changed |= c
        return node, changed
    if isinstance(node, list):
        for i, value in enumerate(node):
            node[i], c = repair_slot_lists(value)
            changed |= c
        return node, changed
    return node, changed


def main():
    apply = "--apply" in sys.argv
    converted = flattened = dropped_cond = dropped_ns = 0
    written = 0
    removed = []
    for path in sorted(DATA.rglob("*.json")):
        if "recipes" not in path.parts:
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue
        if not isinstance(data, dict):
            continue

        extra = []
        data = flatten(data, extra, [], [])
        dropped_cond += 0
        if data is None:
            dropped_cond += 1
            if apply:
                path.unlink()
            removed.append(path)
            continue
        data, conv = convert_ingredients(data)
        data, repaired = repair_slot_lists(data)
        conv |= repaired
        data, conds = strip_conditions(data)
        if unknown_namespaces(data):
            dropped_ns += 1
            if apply:
                path.unlink()
            removed.append(path)
            continue
        if conv or conds:
            converted += 1
            if apply:
                path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
                written += 1
        if extra:
            recipes = []
            for item in extra:
                recipes.extend(item if isinstance(item, list) else [item])
            if apply:
                stem = path.stem
                for idx, recipe in enumerate(recipes):
                    target = path.with_name(f"{stem}__{idx}.json")
                    recipe, _ = convert_ingredients(recipe)
                    target.write_text(json.dumps(recipe, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
                # the original file still holds the conditional wrapper, so it has to go
                path.unlink()
            flattened += len(recipes)

    print(f"ingredient conversions: {converted}")
    print(f"extra flattened recipe files: {flattened}")
    print(f"files removed (condition false or compat): {len(removed)}")
    print(f"dropped unknown namespace: {dropped_ns}")
    if not apply:
        print("dry run; pass --apply to write")
    print("sample removed:", [str(p.relative_to(ROOT)) for p in removed[:5]])


if __name__ == "__main__":
    main()
