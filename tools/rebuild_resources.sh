#!/usr/bin/env bash
# Rebuild the ported resource tree from the Forge jar: extract, bake the Forge-only
# model loaders into vanilla ones, strip atlas sources Fabric cannot parse, then
# regenerate the compatibility tags and cast recipes this port adds.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="C:/Users/YamIpass/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies/python/python.exe"

[ -d /tmp/keep312 ] || { echo "missing /tmp/keep312 backups"; exit 1; }

"$PY" tools/extract_312.py
# the block model bake stays: the block loads of the smeltery are still being ported
"$PY" tools/bake_block_models.py
# the bake drops the forge and mantle "loader" keys, put the upstream models back wherever a loader was lost
"$PY" tools/restore_block_model_loaders.py --apply
# item models must be the upstream ones, the ported material/tool/fluid_container loaders do the work now
"$PY" tools/restore_upstream_item_models.py
"$PY" tools/extract_tags.py
"$PY" tools/extract_extra_data.py
"$PY" tools/unwrap_advancements.py
"$PY" - <<'EOF'
import json, glob
for p in glob.glob("src/main/resources/assets/minecraft/atlases/*.json"):
    d = json.load(open(p, encoding="utf-8"))
    srcs = d.get("sources", [])
    d["sources"] = [s for s in srcs if str(s.get("type", "")).startswith("minecraft:")]
    json.dump(d, open(p, "w", encoding="utf-8"), indent=2)
    print(p, "kept", len(d["sources"]))
EOF
"$PY" tools/gen_forge_tags.py
"$PY" tools/gen_cast_recipes.py
"$PY" tools/add_port_lang.py
"$PY" tools/gen_gui_textures.py
"$PY" tools/patch_blockstates.py
# Forge loot conditions have Porting Lib twins with the same names
"$PY" tools/fix_forge_loot_conditions.py
# Fabric reads our custom ingredients from the `fabric:type` key, the Forge data uses `type`.
# The JSON aware pass is required: a plain textual rewrite also hits recipe roots and model loaders.
"$PY" tools/fix_recipe_ingredients.py --apply
"$PY" tools/normalize_forge_data.py --apply
# Mantle ignores upstream's Forge `conditions`, so foreign mod content has to be dropped like Forge would skip it
"$PY" tools/drop_foreign_effects.py --apply
"$PY" tools/drop_foreign_fluid_transfers.py --apply
# Porting Lib ships the composite model loader under its own namespace
# the upstream pane item models rely on mantle:item_layer tints, which do not work for items here
"$PY" tools/gen_glass_pane_icons.py
"$PY" tools/fix_composite_loaders.py --apply
# Fabric has no extra display context registry, so alias the mod ids onto vanilla ones (see TinkerItemDisplays)
"$PY" tools/fix_display_contexts.py --apply
mkdir -p src/main/resources/data/c/tags/items/dusts
cp -rf /tmp/keep312/c/tags/items/. src/main/resources/data/c/tags/items/
cp -f /tmp/keep312/crafting_repair.json src/main/resources/data/tconstruct/recipes/tools/ 2>/dev/null || true
cp -f /tmp/keep312/pattern.json src/main/resources/data/tconstruct/recipes/tools/parts/ 2>/dev/null || true
echo "resources rebuilt"
