"""Regenerates the ``jars`` array of ``fabric.mod.json`` from the vendored ``libs/`` jars.

Fabric Loader only loads nested mods that the parent mod declares in ``fabric.mod.json`` under
``"jars": [{"file": "META-INF/jars/<name>.jar"}]``. Loom's ``include`` configuration writes that list for
you, but it cannot handle plain file dependencies (it rejects them as "not a module component"), so the
port copies ``libs/`` into ``META-INF/jars`` with a plain jar task and has to keep this list in sync itself.

Run from the project root after adding or removing anything in ``libs/``:

    python tools/gen_mod_jars.py
"""

import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
LIBS = ROOT / "libs"
MOD_JSON = ROOT / "src" / "main" / "resources" / "fabric.mod.json"


def main() -> None:
    jars = [{"file": f"META-INF/jars/{jar.name}"} for jar in sorted(LIBS.glob("*.jar"))]
    data = json.loads(MOD_JSON.read_text(encoding="utf-8"))
    data["jars"] = jars
    # keep the loader requirement in step with what the vendored Mantle needs
    data["depends"]["fabricloader"] = ">=0.17.2"
    MOD_JSON.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"fabric.mod.json now declares {len(jars)} nested jars")


if __name__ == "__main__":
    main()
