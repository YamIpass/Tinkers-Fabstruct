"""Split the current compile log into per-package error reports for the fix agents."""
import os
import re
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOG = sys.argv[1] if len(sys.argv) > 1 else '/tmp/n3.log'
OUT = os.path.join(HERE, 'build', 'batches')

LINE = re.compile(r'src[\\/]main[\\/]java[\\/]slimeknights[\\/]tconstruct[\\/](.+?)\.java:(\d+): 错误: (.*)')

BUCKETS = [
    ('library-tools', ('library/tools/',)),
    ('library-recipe', ('library/recipe/', 'library/json/')),
    ('library-client', ('library/client/',)),
    ('library-modifiers', ('library/modifiers/', 'library/materials/', 'library/fluid/', 'library/utils/',
                           'library/events/', 'library/recipe/../')),
    ('smeltery', ('smeltery/',)),
    ('tools', ('tools/',)),
    ('world-fluids-common', ('world/', 'fluids/', 'common/')),
    ('tables-gadgets-other', ('tables/', 'gadgets/', 'shared/', 'TConstruct.java')),
]


def bucket_of(rel):
    rel = rel.replace('\\', '/')
    for name, prefixes in BUCKETS:
        for prefix in prefixes:
            if rel.startswith(prefix):
                return name
    return 'tables-gadgets-other'


def main():
    groups = defaultdict(lambda: defaultdict(list))
    with open(LOG, encoding='utf-8', errors='replace') as handle:
        for line in handle:
            match = LINE.search(line)
            if not match:
                continue
            rel, lineno, message = match.group(1), match.group(2), match.group(3)
            groups[bucket_of(rel)][rel].append((int(lineno), message.strip()))

    os.makedirs(OUT, exist_ok=True)
    for name in sorted(groups):
        files = groups[name]
        total = sum(len(v) for v in files.values())
        path = os.path.join(OUT, name + '.txt')
        with open(path, 'w', encoding='utf-8') as out:
            out.write('%s: %d errors in %d files\n\n' % (name, total, len(files)))
            for rel in sorted(files, key=lambda k: -len(files[k])):
                out.write('%s  (%d)\n' % (rel, len(files[rel])))
                for lineno, message in files[rel][:240]:
                    out.write('    :%d %s\n' % (lineno, message))
                out.write('\n')
        print('%-22s %5d errors %4d files -> build/batches/%s.txt' % (name, total, len(files), name))
    return 0


if __name__ == '__main__':
    sys.exit(main())
