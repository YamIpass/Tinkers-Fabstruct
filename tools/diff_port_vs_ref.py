import os, sys

ref = 'ref/TinkersConstruct/src/main/java/slimeknights/tconstruct'
port = 'tconstruct-fabric/src/main/java/slimeknights/tconstruct'


def rels(root):
    out = set()
    for dp, _, fs in os.walk(root):
        for f in fs:
            if f.endswith('.java'):
                rel = os.path.relpath(os.path.join(dp, f), root)
                out.add(rel.replace(os.sep, '/'))
    return out


r = rels(ref)
p = rels(port)
extra = sorted(p - r)
missing = sorted(r - p)
print('port-only files:', len(extra))
for e in extra:
    print('  ', e)
print('still-missing upstream files:', len(missing))
for e in missing[:40]:
    print('  -', e)
