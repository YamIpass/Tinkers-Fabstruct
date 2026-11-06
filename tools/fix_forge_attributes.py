"""Map Forge attribute ids in data files onto the Fabric-side equivalents.

Forge shipped extra attributes (entity_gravity, swim_speed, ...). Porting Lib registers
the same attributes under the `porting_lib` namespace (its mixins apply them), and
reach-entity-attributes provides the reach pair, so the ids only need remapping.

Usage: python tools/fix_forge_attributes.py [--apply]
"""
import argparse
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / 'src/main/resources/data'

MAPPING = {
    'forge:entity_gravity': 'porting_lib:entity_gravity',
    'forge:swim_speed': 'porting_lib:swim_speed',
    'forge:step_height_addition': 'porting_lib:step_height_addition',
    'forge:entity_reach': 'reach-entity-attributes:reach',
    'forge:block_reach': 'reach-entity-attributes:reach',
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    changed = 0
    for path in DATA.rglob('*.json'):
        text = path.read_text(encoding='utf-8')
        new = text
        for old, replacement in MAPPING.items():
            new = new.replace(f'"attribute": "{old}"', f'"attribute": "{replacement}"')
        if new != text:
            changed += 1
            print(f'{"fix" if args.apply else "would fix"}: {path.relative_to(DATA)}')
            if args.apply:
                path.write_text(new, encoding='utf-8')
    print(f'changed files: {changed}')


if __name__ == '__main__':
    main()
