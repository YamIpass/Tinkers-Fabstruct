"""Revert the bad `action` -> `transaction` rename inside the fluid effect records.

An earlier batch replace rewrote the `TimeAction` record component of several fluid
effect loadables from `action` to `transaction`, which made every fluid effect JSON
fail with `Missing JSON field 'transaction'`. The upstream field name is `action`.

Usage: python tools/fix_time_action_field.py [--apply]
"""
import argparse
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
FILES = [
    'library/modifiers/fluid/entity/FireFluidEffect.java',
    'library/modifiers/fluid/entity/FreezeFluidEffect.java',
    'library/modifiers/fluid/entity/MobEffectFluidEffect.java',
    'library/modifiers/fluid/FluidMobEffect.java',
    'library/modifiers/fluid/general/AreaMobEffectFluidEffect.java',
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    for rel in FILES:
        path = ROOT / 'src/main/java/slimeknights/tconstruct' / rel
        text = path.read_text(encoding='utf-8')
        new = text.replace('TimeAction transaction', 'TimeAction action')
        new = new.replace('requiredField("transaction", e -> e.transaction)',
                          'requiredField("action", e -> e.action)')
        new = new.replace('@param transaction', '@param action')
        if new != text:
            print(f'{"fix" if args.apply else "would fix"}: {rel}')
            if args.apply:
                path.write_text(new, encoding='utf-8')


if __name__ == '__main__':
    main()
