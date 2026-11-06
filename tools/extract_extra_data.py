"""Extracts every data file of the shipped namespaces that the resource pipeline skipped.

`unzip <pattern>` only matches inside one directory level, so namespace folders with sub folders stayed empty even
though the jar carries the files (advancements, loot tables, worldgen, ...).
"""
import os
import sys
import zipfile

JAR = r"C:\Users\YamIpass\AppData\Roaming\.minecraft\versions\1.20.1-NeoForge_47.1.106\mods\[匠魂] TConstruct-1.20.1-3.12.1.231.jar"
PREFIXES = [
    "data/tconstruct/advancements/",
    "data/tconstruct/loot_tables/",
    "data/tconstruct/worldgen/",
    "data/tconstruct/structures/",
    "data/tconstruct/functions/",
    "data/tconstruct/mantle/",
    "data/tconstruct/damage_type/",
    "data/tconstruct/trim_material/",
    "data/tconstruct/loot_modifiers/",
    "data/tconstruct/command/",
]
OUT = "src/main/resources"

written = 0
with zipfile.ZipFile(JAR) as jar:
    for info in jar.infolist():
        name = info.filename
        if info.is_dir() or not (name.endswith((".json", ".mcfunction", ".nbt"))):
            continue
        if not any(name.startswith(prefix) for prefix in PREFIXES):
            continue
        target = os.path.join(OUT, name)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with jar.open(info) as src, open(target, "wb") as dst:
            dst.write(src.read())
        written += 1
print("written", written)
for prefix in PREFIXES:
    folder = os.path.join(OUT, prefix)
    count = sum(len(files) for _, _, files in os.walk(folder)) if os.path.isdir(folder) else 0
    print(prefix, count)
