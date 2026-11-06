import os, subprocess, sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
SRC = os.path.join(ROOT, 'src', 'main', 'java')
MERGE = os.path.join(ROOT, 'build', 'merge3')
def norm(p):
    with open(p, 'r', encoding='utf-8', newline='') as f:
        return f.read().replace('\r\n', '\n')
only = sys.argv[1:]
changed, same = [], []
for dirpath, _, files in os.walk(MERGE):
    for f in files:
        if not f.endswith('.java'):
            continue
        mp = os.path.join(dirpath, f)
        rel = os.path.relpath(mp, MERGE)
        if only and rel.replace(os.sep, '/') not in only:
            continue
        dst = os.path.join(SRC, 'slimeknights', 'tconstruct', rel)
        text = norm(mp)
        if not os.path.exists(dst):
            print('MISSING DST', rel); continue
        if norm(dst) != text:
            changed.append(rel)
        else:
            same.append(rel)
print('changed:', len(changed), 'identical:', len(same))
for r in changed:
    print('  ~', r.replace(os.sep, '/'))
for r in same:
    print('  =', r.replace(os.sep, '/'))
