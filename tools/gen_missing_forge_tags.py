"""Writes the `forge:` item tags that Tinkers 3.12.1 references but nothing registered.

On Forge these come from the forge mod (or other mods). Our port has no forge mod, so the
tags are recreated here with the vanilla items Forge would put in them.

Usage: python tools/gen_missing_forge_tags.py [--apply]
"""
import argparse
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
TAGS = ROOT / 'src/main/resources/data/forge/tags/items'

TAGS_TO_WRITE = {
    'stone': [
        'minecraft:stone', 'minecraft:granite', 'minecraft:diorite', 'minecraft:andesite',
        'minecraft:polished_granite', 'minecraft:polished_diorite', 'minecraft:polished_andesite',
        'minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:cracked_stone_bricks',
        'minecraft:chiseled_stone_bricks', 'minecraft:smooth_stone', 'minecraft:deepslate',
        'minecraft:cobbled_deepslate', 'minecraft:polished_deepslate', 'minecraft:deepslate_bricks',
        'minecraft:deepslate_tiles', 'minecraft:tuff', 'minecraft:calcite',
    ],
    'cobblestone': [
        'minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:cobbled_deepslate',
    ],
    'sandstone': [
        'minecraft:sandstone', 'minecraft:chiseled_sandstone', 'minecraft:cut_sandstone',
        'minecraft:smooth_sandstone', 'minecraft:red_sandstone', 'minecraft:chiseled_red_sandstone',
        'minecraft:cut_red_sandstone', 'minecraft:smooth_red_sandstone',
    ],
    'end_stones': ['minecraft:end_stone'],
    'gravel': ['minecraft:gravel'],
    'seeds': [
        'minecraft:wheat_seeds', 'minecraft:pumpkin_seeds', 'minecraft:melon_seeds',
        'minecraft:beetroot_seeds', 'minecraft:torchflower_seeds', 'minecraft:pitcher_pod',
    ],
    # no forge mod provides byproduct rates here; an empty tag keeps tinkers treating ores as singular
    'ore_rates/dense': [],
    'ore_rates/sparse': [],
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    for name, values in TAGS_TO_WRITE.items():
        path = TAGS / f'{name}.json'
        if path.exists():
            print(f'skip (exists) {name}')
            continue
        print(f'{"write" if args.apply else "would write"} {name} ({len(values)} values)')
        if args.apply:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps({'values': values}, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
