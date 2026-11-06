"""Bakes block models that rely on loaders Fabric cannot run.

Upstream table blocks (tinker station, part builder, modifier worktable, anvils) draw themselves with
Mantle's ``mantle:retextured`` loader: it inherits an element layout from a parent and swaps the
texture variables. Vanilla model inheritance does the same thing — a child model's ``textures`` map
overrides the parent's placeholders — so dropping the loader and keeping ``parent`` plus ``textures``
gives an identical result with no custom loading.

Loaders with no vanilla equivalent (``mantle:connected``, ``tconstruct:tank``, ``forge:composite``)
cannot run on Fabric, and a model whose loader is unknown fails to bake at all — the whole block then
renders as the purple/black missing model.  Their element lists are already complete in the JSON (the
loader only adds behaviour such as connected textures or fluid surfaces), so the loader is dropped
here as well; the geometry survives and the block renders.

Run after ``extract_312.py``:

    python tools/bake_block_models.py
"""

from __future__ import annotations

import json
import os
import sys

# Loaders that are pure decoration over the vanilla model system and can be dropped outright.
DROPPABLE = {
    "mantle:retextured",
}

# Extra keys the loaders consume; vanilla ignores them, but they only confuse readers.
DROPPED_KEYS = ("retextured",)


def main(argv: list[str]) -> int:
    root = argv[1] if len(argv) > 1 else "src/main/resources"
    base = os.path.join(root, "assets", "tconstruct", "models", "block")
    if not os.path.isdir(base):
        print(f"no block models under {base}", file=sys.stderr)
        return 1

    counts: dict[str, int] = {}
    for current, _, files in os.walk(base):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(current, name)
            with open(path, encoding="utf-8") as handle:
                try:
                    model = json.load(handle)
                except json.JSONDecodeError:
                    continue
            if not isinstance(model, dict):
                continue
            loader = model.get("loader")
            if loader is None:
                continue
            model.pop("loader", None)
            for key in DROPPED_KEYS:
                model.pop(key, None)
            with open(path, "w", encoding="utf-8") as handle:
                json.dump(model, handle, indent=2)
                handle.write("\n")
            bucket = loader if loader in DROPPABLE else f"stripped:{loader}"
            counts[bucket] = counts.get(bucket, 0) + 1

    print(f"block models: {counts}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
