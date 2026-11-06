"""Add the few lang keys this port needs but upstream does not ship."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/tconstruct/lang"
KEYS = {
    "en_us": {
        "tooltip.tconstruct.tool.materials": "Materials: ",
        "gui.tconstruct.part_builder.selected": "Selected pattern: %s",
    },
    "zh_cn": {
        "tooltip.tconstruct.tool.materials": "材料：",
        "gui.tconstruct.part_builder.selected": "已选择模具：%s",
    },
}

for lang, keys in KEYS.items():
    path = ROOT / f"{lang}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(keys)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"{lang}: {len(keys)} keys")
