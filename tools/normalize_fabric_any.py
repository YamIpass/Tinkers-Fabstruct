"""Flatten nested `fabric:any` ingredients.

The forge -> fabric normalizer wraps ingredients that contain a custom ingredient into
`{"fabric:type": "fabric:any", "ingredients": [...]}`. Running it more than once nests the wrapper,
and Fabric's CombinedIngredient then sees an object where it expects an array. This rewrites every
`fabric:any` node into a canonical wrapper whose `ingredients` is a flat array with all nested
`fabric:any` entries spliced in.

Usage: normalize_fabric_any.py [--apply]
"""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src" / "main" / "resources" / "data"


def is_any(node) -> bool:
    return isinstance(node, dict) and node.get("fabric:type") == "fabric:any"


def canonical_any(entries):
    flat = []
    for entry in entries:
        if is_any(entry):
            inner = entry.get("ingredients")
            if isinstance(inner, list):
                flat.extend(canonical_any(inner)["ingredients"])
                continue
            if isinstance(inner, dict):
                flat.extend(canonical_any([inner])["ingredients"])
                continue
        flat.append(entry)
    return {"fabric:type": "fabric:any", "ingredients": flat}


def normalize(value):
    if isinstance(value, list):
        return [normalize(entry) for entry in value]
    if isinstance(value, dict):
        if is_any(value):
            inner = value.get("ingredients")
            if isinstance(inner, list):
                return canonical_any([normalize(entry) for entry in inner])
            if isinstance(inner, dict):
                return canonical_any([normalize(inner)])
        return {key: normalize(sub) for key, sub in value.items()}
    return value


def main(apply: bool) -> None:
    changed = 0
    for path in sorted(DATA.rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        if "fabric:any" not in text:
            continue
        data = json.loads(text)
        updated = normalize(data)
        if json.dumps(updated, sort_keys=True) == json.dumps(data, sort_keys=True):
            continue
        changed += 1
        if apply:
            path.write_text(json.dumps(updated, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"flattened fabric:any files: {changed}")
    if not apply:
        print("dry run, pass --apply to write")


if __name__ == "__main__":
    main("--apply" in sys.argv)
