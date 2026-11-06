"""Adopt the Fabric-adapted files from the reference port (Hephaestus 3.6.4).

For the files whose decompiled 3.6.4 and 3.12.1 bodies are byte-identical
(build/heph_unchanged.txt), the upstream 3.6.4 -> 3.12.1 delta is empty, so the
port's Fabric-adapted version is exactly what our verbatim copy needs.
"""
import os
import shutil
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WS = os.path.dirname(HERE)
SRC = os.path.join(HERE, 'src', 'main', 'java')
REF = os.path.join(WS, 'ref', 'Hephaestus-1.20.1', 'src', 'main', 'java')
LIST = os.path.join(HERE, 'build', 'heph_unchanged.txt')


def main():
    copied = 0
    missing = []
    with open(LIST, encoding='utf-8') as handle:
        paths = [line.strip() for line in handle if line.strip()]
    for rel in paths:
        target = os.path.join(SRC, rel.replace('/', os.sep))
        source = os.path.join(REF, rel.replace('/', os.sep))
        if not os.path.isfile(source):
            missing.append(rel)
            continue
        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.copyfile(source, target)
        copied += 1
    print('adopted %d files' % copied)
    if missing:
        print('missing in reference: %d' % len(missing))
        for rel in missing[:10]:
            print('  ' + rel)
    return 0


if __name__ == '__main__':
    sys.exit(main())
