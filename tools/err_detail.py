"""Print javac errors (file, line, message) filtered by path substrings.

Usage: python tools/err_detail.py <log> [substr ...]
"""
import re
import sys

log = sys.argv[1]
filters = sys.argv[2:]
txt = open(log, encoding='utf-8', errors='replace').read()
pat = re.compile(r'^(.*?\.java):(\d+): (?:错误|error): (.*)$', re.M)
for m in pat.finditer(txt):
    path = m.group(1).replace('\\', '/')
    if filters and not any(f in path for f in filters):
        continue
    short = path.split('tconstruct/')[-1]
    print(f'{short}:{m.group(2)} {m.group(3)[:140]}')
