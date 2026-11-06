"""Extracts every tag namespace of the upstream jar.

The earlier extract only kept `tags/items` and `tags/fluids`, so block tags such as `tconstruct:slimy_logs` were missing
and vanilla tags like `minecraft:leaves` failed to load, which in turn broke several vanilla tag driven mechanics.
"""
import os
import sys
import zipfile

JAR = r"C:\Users\YamIpass\AppData\Roaming\.minecraft\versions\1.20.1-NeoForge_47.1.106\mods\[匠魂] TConstruct-1.20.1-3.12.1.231.jar"
DEST = os.path.join("src", "main", "resources")
PREFIX = "data/tconstruct/tags/"


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
    print("tags written", written)


if __name__ == "__main__":
    main()
