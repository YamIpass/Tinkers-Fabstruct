"""Reports items that have a lang entry but no item model and no blockstate.

Blank creative tab slots usually mean the item resolved to the missing model, which happens when the port forgot to
bake a model for an item (the upstream jar used Forge model loaders that never made it into our resource pipeline).
"""
import io
import json
import os

ROOT = "src/main/resources/assets/tconstruct"
LANG = os.path.join(ROOT, "lang", "en_us.json")

with io.open(LANG, encoding="utf-8") as fp:
    lang = json.load(fp)

missing = []
for key in lang:
    for prefix in ("item.tconstruct.", "block.tconstruct."):
        if key.startswith(prefix):
            name = key[len(prefix):]
            if "." in name:
                # tooltip/description keys, not item names
                continue
            item_model = os.path.join(ROOT, "models", "item", name + ".json")
            blockstate = os.path.join(ROOT, "blockstates", name + ".json")
            if not os.path.exists(item_model) and not os.path.exists(blockstate):
                missing.append((key, name))

missing.sort()
print("items with neither item model nor blockstate:", len(missing))
for key, name in missing[:40]:
    print(" ", key)
