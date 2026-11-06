"""Quantify how much of the v3.12.1 source can be adopted verbatim from the
Fabric port (Hephaestus 3.6.4) by comparing the decompiled Forge 3.6.4 tree
with our decompiled Forge 3.12.1 tree.

Paths are relative to src/main/java in each tree.
"""
import hashlib
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WS = os.path.dirname(ROOT)

TREES = {
    'forge364': os.path.join(WS, 'ref', 'TConstruct-3.6.4', 'src', 'main', 'java'),
    'forge3121': os.path.join(WS, 'ref', 'TinkersConstruct', 'src', 'main', 'java'),
    'fabric364': os.path.join(WS, 'ref', 'Hephaestus-1.20.1', 'src', 'main', 'java'),
}


def scan(path):
    out = {}
    for dirpath, _, files in os.walk(path):
        for name in files:
            if not name.endswith('.java'):
                continue
            full = os.path.join(dirpath, name)
            rel = os.path.relpath(full, path).replace(os.sep, '/')
            with open(full, 'rb') as handle:
                data = handle.read()
            out[rel] = (hashlib.sha256(data).hexdigest(), len(data))
    return out


def main():
    trees = {}
    for key, path in TREES.items():
        if not os.path.isdir(path):
            print('missing tree: %s (%s)' % (key, path))
            return 1
        trees[key] = scan(path)
        print('%s: %d files' % (key, len(trees[key])))

    forge364 = trees['forge364']
    forge3121 = trees['forge3121']
    fabric364 = trees['fabric364']

    shared = sorted(set(fabric364) & set(forge364) & set(forge3121))
    only_fabric = sorted(set(fabric364) - set(forge364))
    only_3121 = sorted(set(forge3121) - set(fabric364))

    same = [p for p in shared if forge364[p][0] == forge3121[p][0]]
    diff = [p for p in shared if forge364[p][0] != forge3121[p][0]]

    print('paths in fabric+364+3121: %d' % len(shared))
    print('  unchanged 3.6.4 -> 3.12.1 (adopt Fabric file verbatim): %d (%.1f%%)'
          % (len(same), 100.0 * len(same) / max(1, len(shared))))
    print('  changed 3.6.4 -> 3.12.1 (needs delta work): %d' % len(diff))
    print('files only in fabric port: %d' % len(only_fabric))
    print('files only in 3.12.1: %d' % len(only_3121))

    by_pkg = {}
    for path in same:
        top = '/'.join(path.split('/')[:3])
        by_pkg[top] = by_pkg.get(top, 0) + 1
    print('\nunchanged files by package:')
    for key, value in sorted(by_pkg.items(), key=lambda kv: -kv[1])[:15]:
        print('  %-45s %d' % (key, value))

    with open(os.path.join(ROOT, 'build', 'heph_unchanged.txt'), 'w', encoding='utf-8') as out:
        for path in same:
            out.write(path + '\n')
    with open(os.path.join(ROOT, 'build', 'heph_changed.txt'), 'w', encoding='utf-8') as out:
        for path in diff:
            out.write(path + '\n')
    with open(os.path.join(ROOT, 'build', 'heph_only3121.txt'), 'w', encoding='utf-8') as out:
        for path in only_3121:
            out.write(path + '\n')
    print('\nwrote build/heph_unchanged.txt, build/heph_changed.txt, build/heph_only3121.txt')
    return 0


if __name__ == '__main__':
    sys.exit(main())
