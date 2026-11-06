"""Delete data-generation and optional-mod-integration sources from the port.

The 3.12.1 data files are already baked into src/main/resources, so the datagen providers
(net.minecraft.data.*) and the third-party mod integrations (plugin/**) are dead weight that only
produce compile errors. This is scaffolding, not gameplay code.

Kept on purpose: packages such as tools/data/ModifierIds and library/modifiers/data hold runtime
id/statistics constants even though their name says "data", so the rule is import-based rather than
directory-based.
"""
import os
import re
import sys

SRC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'src', 'main', 'java')
ROOT = os.path.join(SRC, 'slimeknights', 'tconstruct')

DATAGEN_IMPORTS = re.compile(
    r'^import (net\.minecraft\.data\.|net\.minecraftforge\.(data|common\.data)\.|'
    r'net\.minecraftforge\.common\.data\b)',
    re.MULTILINE)


def java_files(root):
    for dirpath, _, names in os.walk(root):
        for name in names:
            if name.endswith('.java'):
                yield os.path.join(dirpath, name)


def main():
    dry_run = '--apply' not in sys.argv
    deleted = []
    for path in java_files(ROOT):
        rel = os.path.relpath(path, SRC).replace(os.sep, '/')
        with open(path, encoding='utf-8', errors='replace') as handle:
            text = handle.read()
        # Only packages that exist purely for data generation. Files outside a /data/ directory
        # (TConstruct.java, the Tinker* module classes) keep their datagen imports for now and get
        # their registration calls removed by hand.
        if '/data/' in rel and DATAGEN_IMPORTS.search(text):
            deleted.append(rel)
            if not dry_run:
                os.remove(path)

    # optional mod integrations: JEI / REI / CraftTweaker / TOP / Curios style plugins
    for dirpath, _, names in os.walk(os.path.join(ROOT, 'plugin')):
        for name in names:
            if not name.endswith('.java'):
                continue
            path = os.path.join(dirpath, name)
            rel = os.path.relpath(path, SRC).replace(os.sep, '/')
            deleted.append(rel)
            if not dry_run:
                os.remove(path)

    print('%s %d files' % ('would delete' if dry_run else 'deleted', len(deleted)))
    for rel in deleted[:15]:
        print('  ' + rel)
    if len(deleted) > 15:
        print('  ... and %d more' % (len(deleted) - 15))
    return 0


if __name__ == '__main__':
    sys.exit(main())
