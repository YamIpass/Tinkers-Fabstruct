"""Copies build/resolved/tools/<rel> into src/main/java/slimeknights/tconstruct/tools/<rel>,
converting to CRLF (all files in the package are CRLF) and validating the result.

Usage: adopt.py <rel> [<rel> ...]
"""
import sys, os

SRC = 'src/main/java/slimeknights/tconstruct/tools'
RES = 'build/resolved/tools'

def adopt(rel):
    path = os.path.join(RES, rel)
    if not os.path.exists(path):
        print('MISSING', path)
        return
    raw = open(path, 'rb').read().decode('utf-8')
    data = raw.replace('\r\n', '\n').replace('\n', '\r\n')
    dest = os.path.join(SRC, rel)
    open(dest, 'wb').write(data.encode('utf-8'))
    text = raw
    problems = []
    for token in ('<<<<<<<', '=======\n', '>>>>>>>', 'net.minecraftforge'):
        if token in text:
            problems.append(token)
    print('adopted', rel, len(text.split('\n')), 'lines', ('PROBLEMS: ' + ','.join(problems)) if problems else 'clean')

for rel in sys.argv[1:]:
    adopt(rel)
