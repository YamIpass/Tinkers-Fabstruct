"""Routes Tinkers attribute reads through a null safe helper.

Forge added the mod attributes to every living entity; the Fabric port only rebuilds the
player's attribute supplier, so mobs crash with "Can't find attribute
tconstruct:generic.safe_fall_distance" the first time they take fall damage. The helper
falls back to the attribute default, which is the value a mob has on Forge too.

Usage: python tools/fix_attribute_reads.py [--apply]
"""
import argparse
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / 'src/main/java'

# entity.getAttributeValue(TinkerAttributes.THING.get()) -> TinkerAttributes.getValue(entity, TinkerAttributes.THING)
PATTERN = re.compile(r'(\w+(?:\.\w+\([^()]*\))?)\.getAttributeValue\(TinkerAttributes\.(\w+)\.get\(\)\)')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    total = 0
    for path in SRC.rglob('*.java'):
        text = path.read_text(encoding='utf-8')
        new, count = PATTERN.subn(lambda m: f'TinkerAttributes.getValue({m.group(1)}, TinkerAttributes.{m.group(2)})', text)
        if count:
            total += count
            print(f'{"fix" if args.apply else "would fix"} {count:2d}  {path.relative_to(SRC)}')
            if args.apply:
                path.write_text(new, encoding='utf-8')
    print(f'total replaced: {total}')


if __name__ == '__main__':
    main()
