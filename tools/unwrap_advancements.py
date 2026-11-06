"""Unwraps Forge datagen advancement batches into vanilla advancement files.

Forge datagen writes several advancements into one file as `{"advancements": [{"advancement": {...}}, ...]}`, while
vanilla loads one advancement per file with a top level `criteria` object. Recipe unlock advancements are dropped since
they only feed the vanilla recipe book and every recipe is registered anyway.
"""
import io
import json
import os

ROOT = os.path.join("src", "main", "resources", "data", "tconstruct", "advancements")
DROP_PARENTS = ("minecraft:recipes",)


def unwrap():
    written = 0
    dropped = 0
    for folder, _, files in os.walk(ROOT):
        for name in sorted(files):
            if not name.endswith(".json"):
                continue
            path = os.path.join(folder, name)
            with io.open(path, encoding="utf-8") as fp:
                try:
                    data = json.load(fp)
                except ValueError:
                    continue
            if not isinstance(data, dict) or "advancements" not in data:
                continue
            stem = name[:-5]
            kept = 0
            for index, entry in enumerate(data["advancements"]):
                advancement = entry.get("advancement") if isinstance(entry, dict) else None
                if not isinstance(advancement, dict) or "criteria" not in advancement:
                    dropped += 1
                    continue
                if str(advancement.get("parent", "")).startswith(DROP_PARENTS):
                    dropped += 1
                    continue
                target = os.path.join(folder, "%s__%d.json" % (stem, index))
                with io.open(target, "w", encoding="utf-8", newline="\n") as out:
                    json.dump(advancement, out, indent=2)
                    out.write("\n")
                kept += 1
                written += 1
            if kept or dropped:
                os.remove(path)
    print("advancement files written", written, "entries dropped", dropped)


if __name__ == "__main__":
    unwrap()
