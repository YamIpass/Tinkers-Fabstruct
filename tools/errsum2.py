"""Summarize a javac log: errors per file and per message."""
import collections
import re
import sys

LOG = sys.argv[1] if len(sys.argv) > 1 else '/tmp/p1.log'
LINE = re.compile(r'src[\\/]main[\\/]java[\\/]slimeknights[\\/]tconstruct[\\/](.+?)\.java:(\d+): 错误: (.*)')

counts = collections.Counter()
msgs = collections.Counter()
lines = collections.defaultdict(list)
with open(LOG, encoding='utf-8', errors='replace') as handle:
    for raw in handle:
        match = LINE.search(raw)
        if not match:
            continue
        rel = match.group(1).replace(chr(92), '/')
        counts[rel] += 1
        msgs[match.group(3).strip()] += 1
        lines[rel].append(int(match.group(2)))

print('files: %d  errors: %d' % (len(counts), sum(counts.values())))
for rel, count in counts.most_common(25):
    print('%4d  %s' % (count, rel))
print('--- messages')
for message, count in msgs.most_common(12):
    print('%4d  %s' % (count, message))
