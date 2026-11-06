"""Rewrites Forge loot conditions to their Porting Lib equivalents.

Vanilla ignores unknown condition types but logs a parse error and drops the whole loot table, which is why the slimy
plant loot tables failed to load. Porting Lib registers the same conditions under its own namespace.
"""
import io
import os

ROOT = os.path.join("src", "main", "resources", "data")
REPLACEMENTS = {
    '"condition": "forge:can_tool_perform_action"': '"condition": "porting_lib:can_tool_perform_action"',
    '"condition": "forge:loot_table_id"': '"condition": "porting_lib:loot_table_id"',
}


def main():
    changed = 0
    for folder, _, files in os.walk(ROOT):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(folder, name)
            with io.open(path, encoding="utf-8") as fp:
                text = fp.read()
            updated = text
            for old, new in REPLACEMENTS.items():
                updated = updated.replace(old, new)
            if updated != text:
                with io.open(path, "w", encoding="utf-8", newline="\n") as fp:
                    fp.write(updated)
                changed += 1
    print("files changed", changed)


if __name__ == "__main__":
    main()
