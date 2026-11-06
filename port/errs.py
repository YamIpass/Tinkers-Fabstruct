import io, sys, re

want = sys.argv[1:]
path = 'build/batches/world-fluids-common.txt'
lines = io.open(path, encoding='utf-8').read().splitlines()
cur = None
out = []
for line in lines:
    if not line.startswith(' ') and line.strip():
        cur = line.strip()
        continue
    if cur and any(w in cur for w in want):
        if line.strip():
            out.append(cur + ' | ' + line.strip())
last = None
for o in out:
    f = o.split(' | ')[0]
    if f != last:
        print('\n== ' + f)
        last = f
    print('   ' + o.split(' | ')[1])
