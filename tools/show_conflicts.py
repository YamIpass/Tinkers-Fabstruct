import os, sys, re
MERGE = os.path.join(os.path.dirname(__file__), '..', 'build', 'merge3')
MERGE = os.path.abspath(MERGE)
pat = sys.argv[1] if len(sys.argv) > 1 else ''
for dirpath, _, files in os.walk(MERGE):
    for f in sorted(files):
        if not f.endswith('.java'):
            continue
        p = os.path.join(dirpath, f)
        rel = os.path.relpath(p, MERGE).replace(os.sep, '/')
        if pat and not re.search(pat, rel):
            continue
        lines = open(p, encoding='utf-8').read().replace('\r\n', '\n').split('\n')
        i = 0
        n = 0
        while i < len(lines):
            if lines[i].startswith('<<<<<<< '):
                start = i
                body = []
                while not lines[i].startswith('>>>>>>> '):
                    body.append(lines[i]); i += 1
                body.append(lines[i])
                n += 1
                print(f'=== {rel} hunk{n} line {start+1}')
                print('\n'.join(body[:40]))
                print()
            i += 1
