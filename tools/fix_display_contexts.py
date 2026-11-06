"""Replace the mod's custom ItemDisplayContext ids in Mantle model item lists.

Upstream registered extra display contexts through Forge's registry. Fabric has none, so
`slimeknights/tconstruct/library/TinkerItemDisplays.java` aliases them onto vanilla contexts;
the JSON files have to be aliased the same way.

Usage: python tools/fix_display_contexts.py [--apply]
"""
import argparse
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'src/main/resources/assets'

# mirrors TinkerItemDisplays
ALIASES = {
    'tconstruct:table': 'none',
    'tconstruct:melter': 'none',
    'tconstruct:casting_basin': 'none',
    'tconstruct:casting_table': 'fixed',
    'tconstruct:fluid_cannon': 'fixed',
    'tconstruct:thrown': 'fixed',
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    changed = 0
    for path in ASSETS.rglob('*.json'):
        text = path.read_text(encoding='utf-8')
        new = text
        for old, alias in ALIASES.items():
            new = new.replace(f'"transform": "{old}"', f'"transform": "{alias}"')
        if new != text:
            changed += 1
            print(f'{"fix" if args.apply else "would fix"}: {path.relative_to(ASSETS)}')
            if args.apply:
                path.write_text(new, encoding='utf-8')
    print(f'changed files: {changed}')


if __name__ == '__main__':
    main()
