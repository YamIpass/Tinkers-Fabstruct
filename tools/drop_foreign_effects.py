"""Drop fluid effect entries that reference content from mods we do not ship.

Upstream guarded these with Forge conditions (mod_loaded); the Fabric port has no such
gate, so entries pointing at e.g. `immersiveengineering:flammable` must be removed or the
whole fluid effect file fails to load.

Usage: python tools/drop_foreign_effects.py [--apply]
"""
import argparse
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / 'src/main/resources/data'
KNOWN = {'minecraft', 'tconstruct', 'c', 'forge', 'porting_lib', 'mantle', 'fabric'}


def foreign(value):
    if not isinstance(value, str) or ':' not in value:
        return False
    return value.split(':', 1)[0] not in KNOWN


def prune(entries):
    if not isinstance(entries, list):
        return entries, 0
    kept, removed = [], 0
    for entry in entries:
        if isinstance(entry, dict) and any(foreign(entry.get(key)) for key in ('effect', 'attribute', 'item', 'block', 'fluid')):
            removed += 1
            continue
        if isinstance(entry, dict):
            # modules may nest another effect list (e.g. tconstruct:mob_effect_cloud. effects)
            entry, nested = prune_effects(entry)
            removed += nested
        kept.append(entry)
    return kept, removed


def prune_effects(node):
    """Recursively drops foreign entries from every nested 'effects' list."""
    removed = 0
    for key, value in list(node.items()):
        if key == 'effects':
            node[key], count = prune(value)
            removed += count
        elif isinstance(value, dict):
            value, count = prune_effects(value)
            removed += count
        elif isinstance(value, list):
            for item in value:
                if isinstance(item, dict):
                    item, count = prune_effects(item)
                    removed += count
    return node, removed


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    total = 0
    for path in DATA.rglob('fluid_effects/*.json'):
        data = json.loads(path.read_text(encoding='utf-8'))
        changed = False
        for key in ('effects', 'entity_effects', 'block_effects'):
            if key in data:
                data[key], removed = prune(data[key])
                if removed:
                    total += removed
                    changed = True
        if changed:
            print(f'{"fix" if args.apply else "would fix"}: {path.relative_to(DATA)}')
            if args.apply:
                path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    print(f'removed entries: {total}')


if __name__ == '__main__':
    main()
