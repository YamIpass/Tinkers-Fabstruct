"""Switches the three data loaders to ConditionHelper so Forge/Mantle conditions are honoured.

The previous code only looked for Fabric's `fabric:load_conditions` key, so the `conditions` field used by the
upstream data files was ignored entirely and content from uninstalled mods was still parsed.
"""
import io

REPLACEMENTS = [
    (
        "src/main/java/slimeknights/tconstruct/library/json/loot/equipment/MobEquipmentManager.java",
        [
            (
                "        // skip if conditions fail\n"
                "        // TODO(fabric): Forge evaluated the \"conditions\" field with the load context; Fabric resolves them before this listener runs\n"
                "        if (json.has(ResourceConditions.CONDITIONS_KEY) && !CraftingHelper.getConditionPredicate(GsonHelper.getAsJsonObject(json, ResourceConditions.CONDITIONS_KEY)).test(json)) {\n"
                "          continue;\n"
                "        }\n",
                "        // skip if conditions fail\n"
                "        // TODO(fabric): Forge evaluated the \"conditions\" field with the load context, the loader port evaluates it in ConditionHelper\n"
                "        if (!ConditionHelper.testConditions(json)) {\n"
                "          continue;\n"
                "        }\n",
            )
        ],
    ),
    (
        "src/main/java/slimeknights/tconstruct/library/modifiers/fluid/FluidEffectManager.java",
        [
            (
                "        // want to parse condition without parsing effects, as the effect serializer may be missing\n"
                "        // TODO(fabric): Fabric evaluates resource conditions ahead of time, so this only guards effects loaded directly\n"
                "        if (json.has(ResourceConditions.CONDITIONS_KEY) && !CraftingHelper.getConditionPredicate(GsonHelper.getAsJsonObject(json, ResourceConditions.CONDITIONS_KEY)).test(json)) {\n"
                "          continue;\n"
                "        }\n",
                "        // want to parse condition without parsing effects, as the effect serializer may be missing\n"
                "        // TODO(fabric): Fabric evaluates resource conditions ahead of time, so this only guards effects loaded directly\n"
                "        if (!ConditionHelper.testConditions(json)) {\n"
                "          continue;\n"
                "        }\n",
            )
        ],
    ),
    (
        "src/main/java/slimeknights/tconstruct/library/modifiers/ModifierManager.java",
        [
            (
                "      // TODO(fabric): Fabric evaluates resource conditions ahead of time, so this only guards modifiers loaded directly\n"
                "      if (json.has(ResourceConditions.CONDITIONS_KEY) && !CraftingHelper.getConditionPredicate(GsonHelper.getAsJsonObject(json, ResourceConditions.CONDITIONS_KEY)).test(json)) {\n"
                "        return null;\n"
                "      }\n",
                "      // TODO(fabric): Fabric evaluates resource conditions ahead of time, so this only guards modifiers loaded directly\n"
                "      if (!ConditionHelper.testConditions(json)) {\n"
                "        return null;\n"
                "      }\n",
            )
        ],
    ),
]

IMPORTS = [
    ("src/main/java/slimeknights/tconstruct/library/json/loot/equipment/MobEquipmentManager.java",
     "import slimeknights.tconstruct.library.utils.JsonUtils;" if False else None),
]

changed = 0
for path, pairs in REPLACEMENTS:
    text = io.open(path, encoding="utf-8").read()
    for old, new in pairs:
        if old not in text:
            print("MISS", path)
            continue
        text = text.replace(old, new)
        changed += 1
    if "import slimeknights.tconstruct.library.utils.ConditionHelper;" not in text:
        anchor = "import slimeknights.tconstruct.TConstruct;"
        if anchor in text:
            text = text.replace(anchor, anchor + "\nimport slimeknights.tconstruct.library.utils.ConditionHelper;", 1)
        else:
            print("NO ANCHOR", path)
    io.open(path, "w", encoding="utf-8", newline="").write(text)
print("replacements", changed)
