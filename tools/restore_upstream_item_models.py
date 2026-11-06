"""Restores the upstream item models that the prototype resource bake flattened.

The bake turned every `tconstruct:material` / `tconstruct:tool` / `tconstruct:fluid_container` model into a plain
`minecraft:item/generated` model with layer textures, which saved the Forge model loaders from being required but threw
away the material tinting: every tool and part rendered in the raw silver-grey sprite colour. The loaders are ported now
(MaterialModel, ToolModel, FluidContainerModel are registered through Porting Lib's geometry loader callback), so the
upstream JSON should be used as is.

Only `models/item/**` is restored; block states and block models keep the baked variants for now.
"""
import os
import sys
import zipfile

JAR = r"C:\Users\YamIpass\AppData\Roaming\.minecraft\versions\1.20.1-NeoForge_47.1.106\mods\[匠魂] TConstruct-1.20.1-3.12.1.231.jar"
DEST = os.path.join("src", "main", "resources")
PREFIX = "assets/tconstruct/models/item/"


def main():
    if not os.path.exists(JAR):
        sys.exit("jar not found: " + JAR)
    written = 0
    with zipfile.ZipFile(JAR) as jar:
        for name in jar.namelist():
            if not name.startswith(PREFIX) or not name.endswith(".json"):
                continue
            target = os.path.join(DEST, *name.split("/"))
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "wb") as fp:
                fp.write(jar.read(name))
            written += 1
    print("item models restored", written)


if __name__ == "__main__":
    main()
