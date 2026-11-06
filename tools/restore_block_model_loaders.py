#!/usr/bin/env python3
"""Restores upstream block models that use a custom model loader.

The early baking step (`bake_block_models.py`) rewrote every block model it could not
bake into a plain vanilla model, which silently dropped the `loader` key.  Now that the
loaders are ported (Mantle's connected/retextured/colored_block, TConstruct's tank,
fluid_texture, material_block, gui) the upstream JSON can be used verbatim again.

Usage: restore_block_model_loaders.py [--apply]
"""
import json
import pathlib
import sys
import zipfile

JAR = pathlib.Path(r"C:/Users/YamIpass/AppData/Roaming/.minecraft/versions/1.20.1-NeoForge_47.1.106/mods/[匠魂] TConstruct-1.20.1-3.12.1.231.jar")
ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
PREFIX = "assets/tconstruct/models/block/"


def main() -> int:
    apply = "--apply" in sys.argv
    jar = zipfile.ZipFile(JAR)
    restored, missing, kept = 0, 0, 0
    for name in jar.namelist():
        if not (name.startswith(PREFIX) and name.endswith(".json")):
            continue
        try:
            upstream = json.loads(jar.read(name))
        except Exception:
            continue
        loader = upstream.get("loader")
        if not loader:
            continue
        target = RES / name
        if not target.exists():
            missing += 1
        current = {}
        if target.exists():
            try:
                current = json.loads(target.read_text(encoding="utf-8"))
            except Exception:
                current = {}
        if current.get("loader") == loader:
            kept += 1
            continue
        if apply:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(upstream, indent=2) + "\n", encoding="utf-8")
        restored += 1
    print(f"models with loader restored: {restored}")
    print(f"already correct: {kept}")
    print(f"missing file (written fresh): {missing}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
