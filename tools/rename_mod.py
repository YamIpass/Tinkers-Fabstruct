"""Renames the mod's display name in one command.

The mod id stays `tconstruct` on purpose: data packs, tags, recipes and worlds are keyed on that
namespace, so renaming it would break every existing save. Only the *display* name changes.

What gets rewritten:
  * `gradle.properties` -> `mod_name`, which `build.gradle` expands into `fabric.mod.json`,
    so the Mods list, F3 debug screen, crash reports and Jade's fallback all follow it
  * `assets/tconstruct/lang/en_us.json` + `zh_cn.json` -> `modmenu.nameTranslation.tconstruct`
    (Jade and ModMenu look this key up *before* the metadata name) and `key.categories.tconstruct`

Usage:
    python tools/rename_mod.py "Tinkers' Fabstruct"
    python tools/rename_mod.py "Fabstruct" --item-groups "匠魂丨"

`--item-groups` also rewrites every creative tab label that starts with the given prefix, e.g.
`匠魂丨常规` becomes `Fabstruct丨常规` when you pass `--item-groups "匠魂丨"`.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
GRADLE_PROPERTIES = ROOT / "gradle.properties"
LANG_DIR = ROOT / "src/main/resources/assets/tconstruct/lang"


def set_gradle_name(name: str) -> None:
    text = GRADLE_PROPERTIES.read_text(encoding="utf-8")
    if re.search(r"^mod_name=.*$", text, flags=re.MULTILINE):
        text = re.sub(r"^mod_name=.*$", lambda _: f"mod_name={name}", text, count=1, flags=re.MULTILINE)
    else:
        text = text.rstrip("\n") + f"\nmod_name={name}\n"
    GRADLE_PROPERTIES.write_text(text, encoding="utf-8")


def rewrite_lang(name: str, item_group_prefix: str | None, item_group_name: str | None) -> None:
    for path in sorted(LANG_DIR.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8-sig"))
        except json.JSONDecodeError as error:
            print(f"skipping {path.name}: {error}")
            continue
        changed = False
        if "modmenu.nameTranslation.tconstruct" in data:
            data["modmenu.nameTranslation.tconstruct"] = name
            changed = True
        if "key.categories.tconstruct" in data:
            data["key.categories.tconstruct"] = name
            changed = True
        if item_group_prefix and item_group_name:
            for key, value in list(data.items()):
                if key.startswith("itemGroup.tconstruct.") and isinstance(value, str) and value.startswith(item_group_prefix):
                    data[key] = item_group_name + value[len(item_group_prefix):]
                    changed = True
        if changed:
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Rename the mod display name")
    parser.add_argument("name", help="new display name, e.g. \"Tinkers' Fabstruct\"")
    parser.add_argument("--item-groups", metavar="OLD_PREFIX", help="old creative tab prefix to replace, e.g. 匠魂丨")
    parser.add_argument("--item-group-name", metavar="NEW_PREFIX", help="new creative tab prefix (defaults to the new mod name)")
    args = parser.parse_args()

    set_gradle_name(args.name)
    rewrite_lang(args.name, args.item_groups, args.item_group_name or args.name)
    print(f"mod display name -> {args.name}")
    print("rebuild with ./gradlew build to bake it into fabric.mod.json")


if __name__ == "__main__":
    main()
