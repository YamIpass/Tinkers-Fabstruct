"""Generates the Forge style item tags the extracted Tinkers' Construct data refers to.

The 1.20.1 Tinkers' data is written against Forge's common tags (`forge:ingots/iron`,
`forge:gems/diamond`, ...). Fabric ships `c:` convention tags instead, and 1.5.6 of the
convention tag API uses flat names (`c:iron_ingots`), so the tag paths do not line up.

Rather than rewriting every recipe, this script emits `data/forge/tags/items/*.json` files
whose entries are plain vanilla items. Tags that describe content this port does not ship
(modded metals such as manyullyn or nickel) are skipped on purpose: their recipes should
simply not resolve yet.

Run with the bundled python:  python tools/gen_forge_tags.py
"""
import json
import pathlib

ROOT = pathlib.Path("src/main/resources")
DATA = ROOT / "data" / "tconstruct"
OUT = ROOT / "data" / "forge" / "tags" / "items"

METALS = ["iron", "gold", "copper", "netherite"]
GEMS = {
    "diamond": "diamond",
    "emerald": "emerald",
    "quartz": "quartz",
    "amethyst": "amethyst_shard",
    "lapis": "lapis_lazuli",
    "coal": "coal",
}
DUSTS = {
    "redstone": "redstone",
    "glowstone": "glowstone_dust",
    "gunpowder": "gunpowder",
    "sugar": "sugar",
    "blaze": "blaze_powder",
}
BLOCKS = {
    "iron": "iron_block",
    "gold": "gold_block",
    "copper": "copper_block",
    "netherite": "netherite_block",
    "diamond": "diamond_block",
    "emerald": "emerald_block",
    "lapis": "lapis_block",
    "redstone": "redstone_block",
    "coal": "coal_block",
    "quartz": "quartz_block",
}
ORES = {
    "iron": "iron_ore",
    "gold": "gold_ore",
    "copper": "copper_ore",
    "diamond": "diamond_ore",
    "emerald": "emerald_ore",
    "lapis": "lapis_ore",
    "redstone": "redstone_ore",
    "coal": "coal_ore",
    "quartz": "nether_quartz_ore",
}
SINGLE = {
    "string": ["string"],
    "leather": ["leather"],
    "paper": ["paper"],
    "slimeball": ["slime_ball"],
    "ender_pearls": ["ender_pearl"],
    "ender_pearl": ["ender_pearl"],
    "feather": ["feather"],
    "flint": ["flint"],
    "bone": ["bone"],
    "charcoal": ["charcoal"],
    "coal": ["coal"],
    "brick": ["brick"],
    "clay": ["clay_ball"],
    "glass": ["glass"],
    "sand": ["sand"],
    "red_sand": ["red_sand"],
    "gravel": ["gravel"],
    "gunpowder": ["gunpowder"],
    "arrow": ["arrow"],
    "torch": ["torch"],
    "obsidian": ["obsidian"],
    "netherrack": ["netherrack"],
    "blaze_rod": ["blaze_rod"],
    "blaze_powder": ["blaze_powder"],
    "magma_cream": ["magma_cream"],
    "sugar_cane": ["sugar_cane"],
    "bamboo": ["bamboo"],
    "cactus": ["cactus"],
    "vine": ["vine"],
    "wheat": ["wheat"],
    "egg": ["egg"],
    "seeds": ["wheat_seeds"],
    "mushroom": ["red_mushroom", "brown_mushroom"],
    "glass_panes": ["glass_pane"],
    "rods/wooden": ["stick"],
    "rods/blaze": ["blaze_rod"],
    "rods/breeze": [],
    "dyes": ["white_dye"],
    "dusts": [],
    "gems": [],
    "ingots": [],
    "nuggets": [],
    "ores": [],
}


def mapping_for(path):
    """Returns the item ids a forge tag path stands for, or None when unmapped."""
    if path in SINGLE:
        return SINGLE[path]
    head, _, tail = path.partition("/")
    if not tail:
        return None
    if head == "ingots" and tail in METALS:
        return [f"{tail}_ingot"]
    if head == "nuggets" and tail in ("iron", "gold", "copper"):
        return [f"{tail}_nugget"]
    if head == "storage_blocks" and tail in BLOCKS:
        return [BLOCKS[tail]]
    if head == "gems" and tail in GEMS:
        return [GEMS[tail]]
    if head == "dusts" and tail in DUSTS:
        return [DUSTS[tail]]
    if head == "ores" and tail in ORES:
        item = ORES[tail]
        values = [item]
        if not tail == "quartz":
            values.append(f"deepslate_{item}")
        return values
    if head == "raw_materials" and tail in ("iron", "gold", "copper"):
        return [f"raw_{tail}"]
    if head == "raw_blocks" and tail in ("iron", "gold", "copper"):
        return [f"raw_{tail}_block"]
    return None


def referenced_tags():
    tags = set()

    def walk(node):
        if isinstance(node, dict):
            for key, value in node.items():
                if key == "tag" and isinstance(value, str) and value.startswith("forge:"):
                    tags.add(value[len("forge:"):])
                else:
                    walk(value)
        elif isinstance(node, list):
            for value in node:
                walk(value)

    for path in DATA.rglob("*.json"):
        try:
            walk(json.loads(path.read_text(encoding="utf-8")))
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
    return tags


def main():
    tags = referenced_tags()
    written = 0
    unmapped = []
    for tag in sorted(tags):
        items = mapping_for(tag)
        if not items:
            unmapped.append(tag)
            continue
        target = OUT / f"{tag}.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        body = {"values": [f"minecraft:{item}" for item in items]}
        target.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")
        written += 1
    print(f"referenced forge tags: {len(tags)}")
    print(f"generated: {written}, left unmapped: {len(unmapped)}")
    print("unmapped sample:", ", ".join(unmapped[:20]))


if __name__ == "__main__":
    main()
