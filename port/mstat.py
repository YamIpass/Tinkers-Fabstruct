"""3-way merge stats: base=Forge3.6.4, ours=Forge3.12.1, theirs=Fabric3.6.4 (Hephaestus)."""
import os, re, sys, difflib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WS = os.path.dirname(ROOT)
OURS = os.path.join(ROOT, 'src', 'main', 'java')
T364 = os.path.join(WS, 'ref', 'TConstruct-3.6.4', 'src', 'main', 'java')
T3121 = os.path.join(WS, 'ref', 'TinkersConstruct', 'src', 'main', 'java')
HEPH = os.path.join(WS, 'ref', 'Hephaestus-1.20.1', 'src', 'main', 'java')


def read(p):
    if not os.path.isfile(p):
        return None
    with open(p, encoding='utf-8', errors='replace') as f:
        return f.read()


def hunks(base, other):
    sm = difflib.SequenceMatcher(None, base, other, autojunk=False)
    out = []
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == 'equal':
            continue
        out.append((i1, i2, other[j1:j2]))
    return out


def main():
    batch = sys.argv[1]
    prefix = sys.argv[2]
    with open(os.path.join(ROOT, 'build', 'batches', batch), encoding='utf-8') as f:
        text = f.read()
    pat = r'^(%s[\\/][\w\\/]+)\s+\((\d+)\)' % re.escape(prefix).replace('/', '[\\\\/]')
    for m in re.finditer(pat, text, re.M):
        rel = m.group(1).replace('\\', '/')
        cnt = int(m.group(2))
        javarel = 'slimeknights/tconstruct/' + rel + '.java'
        a = read(os.path.join(T364, javarel))
        b = read(os.path.join(T3121, javarel))
        h = read(os.path.join(HEPH, javarel))
        if a is None or h is None:
            print('%-4d SKIP %s' % (cnt, rel))
            continue
        base = a.splitlines()
        vh = hunks(base, b.splitlines())
        lh = hunks(base, h.splitlines())
        # count overlaps
        ov = 0
        for i1, i2, _ in vh:
            for k1, k2, _ in lh:
                if i1 < k2 and k1 < i2:
                    ov += 1
                    break
        print('%-4d vh=%-4d lh=%-4d ov=%-3d %s' % (cnt, len(vh), len(lh), ov, rel))


main()
