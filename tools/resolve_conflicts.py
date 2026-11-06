"""Resolves diff3 conflict hunks in build/merge3 files with an explicit per-hunk choice string.

Usage: resolve_conflicts.py <rel> <choices>
  rel      path relative to tools/, e.g. logic/EquipmentChangeWatcher.java
  choices  string of 'O' (keep ours) / 'T' (take theirs), one char per hunk, in order

Writes the resolved text to build/resolved/<rel>.
"""
import sys, os

ROOT = 'build/merge3/tools'
OUT = 'build/resolved/tools'

def main():
    rel, choices = sys.argv[1], sys.argv[2].upper()
    path = os.path.join(ROOT, rel)
    raw = open(path, 'rb').read().decode('utf-8')
    lines = raw.split('\n')
    out = []
    idx = 0
    i = 0
    cur = None
    while i < len(lines):
        line = lines[i]
        if line.startswith('<<<<<<<'):
            cur = []
        elif line.startswith('=======') and cur is not None:
            cur.append(None)
        elif line.startswith('>>>>>>>') and cur is not None:
            if idx >= len(choices):
                raise SystemExit('more hunks than choices at hunk %d' % (idx + 1))
            pick = choices[idx]
            body = cur if pick == 'O' else cur[cur.index(None) + 1:]
            if pick == 'O':
                body = cur[:cur.index(None)]
            out.extend(body)
            cur = None
            idx += 1
        elif cur is not None:
            cur.append(line)
        else:
            out.append(line)
        i += 1
    if idx != len(choices):
        raise SystemExit('%d hunks but %d choices' % (idx, len(choices)))
    dest = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    data = '\n'.join(out)
    if not data.endswith('\n'):
        data += '\n'
    with open(dest, 'wb') as f:
        f.write(data.encode('utf-8'))
    print('resolved %d hunks -> %s' % (idx, dest))

if __name__ == '__main__':
    main()
