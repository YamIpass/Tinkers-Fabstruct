"""Rewrite the two smeltery blockstates the port uses into property-free variants.

Upstream blockstates declare properties the port's blocks do not have
(`in_structure`/`active` on the melter, `facing` on the casting tank).  A
blockstate whose variants never match leaves the block unrenderable, so replace
them with a single unconditional variant that points at the existing model.
"""
import json
from pathlib import Path

BLOCKSTATES = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "tconstruct" / "blockstates"

TARGETS = {
    "seared_melter": "tconstruct:block/smeltery/controller/melter_inactive",
    "seared_casting_tank": "tconstruct:block/smeltery/tank/casting_tank",
}


def main():
    for name, model in TARGETS.items():
        path = BLOCKSTATES / f"{name}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        data = {"variants": {"": {"model": model}}}
        path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
        print("patched", path.name, "->", model)


if __name__ == "__main__":
    main()
