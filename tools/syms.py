"""Group javac 'cannot find symbol' errors by the missing symbol text."""
import collections
import re
import sys

log = sys.argv[1] if len(sys.argv) > 1 else '/tmp/q1.log'
text = open(log, encoding='utf-8', errors='replace').read()
blocks = re.split(r'\n(?=\S.*\.java:\d+: 错误: )', text)
groups = collections.Counter()
for block in blocks:
    if '找不到符号' not in block:
        continue
    m = re.search(r'tconstruct-fabric[\\/]src[\\/]main[\\/]java[\\/](.+?\.java):(\d+)', block)
    sym = re.search(r'符号:\s+(?:类|方法|变量)\s+(\S+)', block)
    if m and sym:
        groups[(sym.group(1), m.group(1).split('/')[-1])] += 1

for (symbol, fname), count in groups.most_common(40):
    print(f'{count:3d}  {symbol[:60]:60s}  {fname}')
