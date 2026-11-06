"""Compares the 3.6.4 data this port was built from with the 3.12.1 target jar.

Reads both jars, prints a per-directory file-count diff and the raw text of a few representative
material and tool definition files so schema changes are visible before any loader is rewritten.
"""
import glob
import sys
import zipfile

OLD = r"C:/Users/YamIpass/AppData/Roaming/.minecraft/versions/jh/mods/Hephaestus-1.20.1-3.6.4.305.jar"
NEW = glob.glob(r"C:/Users/YamIpass/AppData/Roaming/.minecraft/versions/1.20.1-NeoForge_47.1.106/mods/"
                r"*TConstruct-1.20.1-3.12.1.231.jar")[0]

SAMPLES = [
    "data/tconstruct/tinkering/materials/stats/iron.json",
    "data/tconstruct/tinkering/materials/definition/iron.json",
    "data/tconstruct/tinkering/tool_definitions/pickaxe.json",
    "data/tconstruct/tinkering/modifiers/diamond.json",
    "data/tconstruct/recipes/tools/parts/builder/pick_head.json",
]


def counts(path):
    with zipfile.ZipFile(path) as jar:
        names = [n for n in jar.namelist() if n.endswith(".json")]
    result = {}
    for name in names:
        for prefix in ("assets/tconstruct/models", "assets/tconstruct/textures", "assets/tconstruct/lang",
                       "data/tconstruct/tinkering/materials/definition", "data/tconstruct/tinkering/materials/stats",
                       "data/tconstruct/tinkering/tool_definitions", "data/tconstruct/tinkering/modifiers",
                       "data/tconstruct/recipes", "data/tconstruct/worldgen"):
            if name.startswith(prefix):
                result[prefix] = result.get(prefix, 0) + 1
                break
    return result


def main():
    old, new = counts(OLD), counts(NEW)
    print(f"{'directory':52} {'3.6.4':>7} {'3.12.1':>7}")
    for key in sorted(set(old) | set(new)):
        print(f"{key:52} {old.get(key, 0):7} {new.get(key, 0):7}")

    with zipfile.ZipFile(NEW) as jar:
        for sample in SAMPLES:
            print("\n=== ", sample)
            try:
                text = jar.read(sample).decode("utf-8")
            except KeyError:
                print("MISSING")
                continue
            print(text[:900])


if __name__ == "__main__":
    main()
