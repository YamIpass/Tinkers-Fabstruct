"""Validate imports of every file in build/batches/tools.txt against our src tree and the libs jars.

Only checks packages we ship ourselves (slimeknights, porting lib, fabric api, alphamode) because
net.minecraft.* comes from the Gradle dependency set and cannot be validated here.
"""
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
SRC = os.path.join(ROOT, 'src', 'main', 'java')
JARINDEX = os.path.join(ROOT, 'build', 'jarindex.txt')
CHECK_PREFIXES = ('slimeknights.', 'io.github.fabricators_of_create.', 'net.fabricmc.', 'me.alphamode.')

JAR = set(open(JARINDEX, encoding='utf-8').read().split('\n'))
SRCTREE = set()
for dirpath, _, fs in os.walk(SRC):
    for f in fs:
        if f.endswith('.java'):
            rel = os.path.relpath(os.path.join(dirpath, f), SRC)
            SRCTREE.add(rel[:-5].replace(os.sep, '.'))


def resolves(cls):
    if cls in SRCTREE or cls in JAR:
        return True
    parts = cls.split('.')
    # nested class written with dots: Outer.Inner -> Outer$Inner
    for i in range(len(parts) - 1, 0, -1):
        outer = '.'.join(parts[:i]) + '$' + '.'.join(parts[i:])
        if outer in JAR or outer in SRCTREE:
            return True
    # nested class of one of our own source files, or a static-imported field/method
    for i in range(len(parts) - 1, 0, -1):
        if '.'.join(parts[:i]) in SRCTREE:
            return True
    return False


def main():
    pat = sys.argv[1] if len(sys.argv) > 1 else ''
    files = []
    for line in open(os.path.join(ROOT, 'build', 'batches', 'tools.txt'), encoding='utf-8'):
        m = re.match(r'^tools.(.*?)\s+\((\d+)\)\s*$', line.rstrip())
        if m:
            files.append('tools/' + m.group(1).replace(os.sep, '/'))
    total = 0
    bad = 0
    for rel in sorted(files):
        if pat and not re.search(pat, rel):
            continue
        p = os.path.join(SRC, 'slimeknights', 'tconstruct', rel + '.java')
        if not os.path.exists(p):
            print('MISSING FILE ' + rel)
            continue
        txt = open(p, encoding='utf-8').read()
        missing = []
        for m in re.finditer(r'^import\s+(?:static\s+)?([\w.]+);', txt, re.M):
            cls = m.group(1)
            if not cls.startswith(CHECK_PREFIXES):
                continue
            if not resolves(cls) and cls not in missing:
                missing.append(cls)
        if missing:
            bad += 1
            total += len(missing)
            print(rel)
            for c in missing:
                print('    ' + c)
    print('files with missing imports: %d   total missing imports: %d' % (bad, total))


main()
