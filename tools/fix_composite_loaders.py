"""Porting Lib registers the composite block model loader as porting_lib:composite, not forge:composite."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODELS = ROOT / "src/main/resources/assets/tconstruct/models"


def main(apply: bool) -> None:
    changed = 0
    for path in sorted(MODELS.rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        if '"forge:composite"' not in text:
            continue
        new_text = text.replace('"forge:composite"', '"porting_lib:composite"')
        # keep the file honest: it still has to be valid json after the rewrite
        json.loads(new_text)
        changed += 1
        if apply:
            path.write_text(new_text, encoding="utf-8")
    print(f"composite loaders fixed: {changed}")
    if not apply:
        print("dry run, pass --apply to write")


if __name__ == "__main__":
    main("--apply" in sys.argv)
