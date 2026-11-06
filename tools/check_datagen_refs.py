"""Find classes in the datagen / mod-integration packages that runtime code still uses.

Deleting pure datagen and optional-mod integration is safe (the data files are already baked into
src/main/resources), but a few classes in those packages are real runtime helpers. This reports
which ones are referenced from outside the candidate set so we keep exactly those.
"""
import os
import re
import sys

SRC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'src', 'main', 'java')
CANDIDATE_DIRS = ('plugin', 'common/data', 'library/data', 'tools/data', 'smeltery/data', 'world/data',
                  'gadgets/data')


def java_files(root):
    for dirpath, _, names in os.walk(root):
        for name in names:
            if name.endswith('.java'):
                yield os.path.join(dirpath, name)


def main():
    root = os.path.join(SRC, 'slimeknights', 'tconstruct')
    inside = []
    for rel in CANDIDATE_DIRS:
        inside.extend(java_files(os.path.join(root, rel.replace('/', os.sep))))
    inside_set = set(os.path.abspath(p) for p in inside)

    texts = {}
    for path in java_files(root):
        with open(path, encoding='utf-8', errors='replace') as handle:
            texts[os.path.abspath(path)] = handle.read()

    outside = {p: t for p, t in texts.items() if p not in inside_set}
    print('candidate files: %d, outside files: %d' % (len(inside_set), len(outside)))

    used = []
    for path in sorted(inside_set):
        name = os.path.basename(path)[:-5]
        if name in ('package-info',):
            continue
        pattern = re.compile(r'\b' + re.escape(name) + r'\b')
        hits = [p for p, t in outside.items() if pattern.search(t)]
        if hits:
            used.append((os.path.relpath(path, SRC), len(hits), os.path.relpath(hits[0], SRC)))
    print('classes still referenced from outside: %d' % len(used))
    for rel, count, sample in used:
        print('  %-75s %2d  e.g. %s' % (rel, count, sample))
    return 0


if __name__ == '__main__':
    sys.exit(main())
