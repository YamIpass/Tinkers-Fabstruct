"""Extract assets/ and data/ trees from the Tinkers' Construct (Hephaestus) jar.

Source jar is the user's own local copy; the mod is MIT licensed.
"""
import sys
import zipfile
from pathlib import Path

JAR = Path(r"C:\Users\YamIpass\AppData\Roaming\.minecraft\versions\jh\mods\Hephaestus-1.20.1-3.6.4.305.jar")
OUT = Path(r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace\tconstruct-fabric\src\main\resources")

PREFIXES = ("assets/", "data/")

count = 0
bytes_written = 0
with zipfile.ZipFile(JAR) as zf:
    for info in zf.infolist():
        name = info.filename
        if info.is_dir() or not name.startswith(PREFIXES):
            continue
        target = OUT / name
        target.parent.mkdir(parents=True, exist_ok=True)
        data = zf.read(info)
        target.write_bytes(data)
        count += 1
        bytes_written += len(data)

print(f"extracted {count} files, {bytes_written/1048576:.1f} MiB")
