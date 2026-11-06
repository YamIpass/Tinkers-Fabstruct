"""Count javac errors of a given message kind per file."""
import collections
import re
import sys

log = sys.argv[1] if len(sys.argv) > 1 else '/tmp/q1.log'
needle = sys.argv[2] if len(sys.argv) > 2 else '方法不会覆盖'
text = open(log, encoding='utf-8', errors='replace').read()
blocks = re.split(r'\n(?=\S.*\.java:\d+: 错误: )', text)
counts = collections.Counter()
for block in blocks:
    if needle not in block:
        continue
    m = re.search(r'java[\\/](.+?\.java):(\d+)', block)
    if m:
        counts[m.group(1).replace(chr(92), '/')] += 1
for name, count in counts.most_common(15):
    print(count, name)
