"""Summarise a javac log: error counts per source file plus the most common messages."""
import collections
import re
import sys

log = sys.argv[1] if len(sys.argv) > 1 else '/tmp/q1.log'
pat = re.compile(r'tconstruct-fabric[\\/]src[\\/]main[\\/]java[\\/](.+?\.java):(\d+): 错误: (.*)')
files = collections.Counter()
msgs = collections.Counter()
lines = collections.defaultdict(list)
for line in open(log, encoding='utf-8', errors='replace'):
    m = pat.search(line)
    if m:
        rel = m.group(1).replace('\\', '/')
        files[rel] += 1
        msgs[m.group(3).strip()[:50]] += 1
        lines[rel].append(int(m.group(2)))

print('files with errors:', len(files), 'total errors:', sum(files.values()))
for name, count in files.most_common(25):
    print(f'{count:5d}  {name}')
print('--- top messages')
for text, count in msgs.most_common(12):
    print(f'{count:5d}  {text}')
