"""Classify error files in an ownership slice against the reference trees."""
import os, re, sys, difflib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WS = os.path.dirname(ROOT)
OURS = os.path.join(ROOT, 'src', 'main', 'java')
T364 = os.path.join(WS, 'ref', 'TConstruct-3.6.4', 'src', 'main', 'java')
T3121 = os.path.join(WS, 'ref', 'TinkersConstruct', 'src', 'main', 'java')
HEPH = os.path.join(WS, 'ref', 'Hephaestus-1.20.1', 'src', 'main', 'java')

batch = sys.argv[1]
prefix = sys.argv[2].replace('\\', '/')  # e.g. slimeknights/tconstruct/library/client


def read(p):
    if not os.path.isfile(p):
        return None
    with open(p, encoding='utf-8', errors='replace') as f:
        return f.read()


def nlines(s):
    return 0 if s is None else s.count('\n')


def dl(a, b):
    """changed line count between two texts"""
    if a is None or b is None:
        return -1
    al = a.splitlines()
    bl = b.splitlines()
    n = 0
    for line in difflib.unified_diff(al, bl, lineterm='', n=0):
        if line.startswith('+++') or line.startswith('---') or line.startswith('@@'):
            continue
        n += 1
    return n


rows = []
with open(os.path.join(ROOT, 'build', 'batches', batch), encoding='utf-8') as f:
    text = f.read()
for m in re.finditer(r'^(%s[\\/][\w\\/]+)\s+\((\d+)\)' % re.escape(prefix).replace('/', '[\\\\/]'), text, re.M):
    rel = m.group(1).replace('\\', '/')
    cnt = int(m.group(2))
    javarel = 'slimeknights/tconstruct/' + rel + '.java'
    ours = read(os.path.join(OURS, javarel))
    a = read(os.path.join(T364, javarel))
    b = read(os.path.join(T3121, javarel))
    h = read(os.path.join(HEPH, javarel))
    tag = []
    if h is None:
        tag.append('NOHEPH')
    if a is None:
        tag.append('NO364')
    if a is not None and b is not None and read(os.path.join(T364, javarel)) == b:
        tag.append('VERUNCHANGED')
    d1 = dl(a, b)      # 3.6.4 forge -> 3.12.1 forge (version delta)
    d2 = dl(a, h)      # 3.6.4 forge -> heph (loader delta)
    d3 = dl(h, ours)   # heph -> ours (what needs merging)
    print('%-6d %-4s %-6s v=%-5s ld=%-5s ho=%-5s %s' % (
        cnt, nlines(ours), nlines(h), d1, d2, d3, rel + ' ' + ','.join(tag)))
