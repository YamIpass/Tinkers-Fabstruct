import json

PY = r"C:/Users/YamIpass/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies/python/python.exe"

pairs = [
    ("src/main/resources/assets/tconstruct/lang/en_us.json",
     {"tooltip.tconstruct.tool.upgrades": "Upgrades: %s / %s",
      "tooltip.tconstruct.tool.abilities": "Abilities: %s / %s"}),
    ("src/main/resources/assets/tconstruct/lang/zh_cn.json",
     {"tooltip.tconstruct.tool.upgrades": "\u5f3a\u5316\u69fd\uff1a%s / %s",
      "tooltip.tconstruct.tool.abilities": "\u80fd\u529b\u69fd\uff1a%s / %s"}),
]

for path, add in pairs:
    with open(path, encoding="utf-8") as handle:
        data = json.load(handle)
    data.update(add)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print("updated", path, len(data))
