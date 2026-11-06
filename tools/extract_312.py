"""Extract assets/ and selected data/ from the Forge 3.12.1 jar into resources.

Only what the Fabric port currently implements is kept: assets, tinkering data and
a handful of recipe trees.  Everything else (worldgen, smeltery, armor, gadgets)
is deferred to later milestones and would only spam load errors.

Source jar is the user's own local copy; the mod is MIT licensed.
"""
import sys
import zipfile
from pathlib import Path

JAR = Path(r"C:\Users\YamIpass\AppData\Roaming\.minecraft\versions\1.20.1-NeoForge_47.1.106\mods\[匠魂] TConstruct-1.20.1-3.12.1.231.jar")
OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources"

DATA_PREFIXES = (
    "data/tconstruct/tinkering/",
    # every recipe: the earlier whitelist silently dropped common/, tables/, smeltery/seared/,
    # smeltery/scorched/, smeltery/entity_melting/, tools/severing/ and the slime skull swapping
    # recipes, which left ~700 recipe files out of the mod
    "data/tconstruct/recipes/",
    # item tags: patterns/default gates the part builder recipes, and the modifiable/* tags are
    # referenced by modifier recipes even though the port ignores the tool restriction
    "data/tconstruct/tags/items/",
    # smeltery: melting and casting recipes plus the fluid tags they resolve against
    "data/tconstruct/recipes/smeltery/melting/",
    "data/tconstruct/recipes/smeltery/casting/",
    "data/tconstruct/recipes/smeltery/casts/",
    "data/tconstruct/recipes/smeltery/alloys/",
    "data/tconstruct/tags/fluids/",
    # tag namespaces the recipes reference: forge:molten_iron and friends live in the upstream jar,
    # and the minecraft/c additions are what vanilla tag files need to merge with
    "data/forge/",
    "data/minecraft/",
    "data/c/",
)


def keep(name: str, info: zipfile.ZipInfo) -> bool:
    if info.is_dir():
        return False
    if name.startswith("assets/"):
        return True
    for namespace in ("forge/", "minecraft/", "c/"):
        if name.startswith("data/" + namespace):
            return True
    if not name.startswith("data/tconstruct/"):
        return False
    return any(name.startswith(p) for p in DATA_PREFIXES)


def main() -> None:
    if not JAR.exists():
        sys.exit(f"missing jar: {JAR}")
    count = 0
    total = 0
    with zipfile.ZipFile(JAR) as zf:
        for info in zf.infolist():
            name = info.filename
            if not keep(name, info):
                continue
            target = OUT / name
            target.parent.mkdir(parents=True, exist_ok=True)
            data = zf.read(info)
            target.write_bytes(data)
            count += 1
            total += len(data)
    print(f"extracted {count} files, {total / 1048576:.1f} MiB")


if __name__ == "__main__":
    main()
