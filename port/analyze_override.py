import re, collections
txt = open('C:/Users/YamIpass/AppData/Local/Temp/portslice/errors.txt', encoding='utf-8', errors='replace').read().split('\n')
c = collections.Counter(); ex = collections.defaultdict(list)
for i, l in enumerate(txt):
    m = re.match(r'^(src.+?\.java):(\d+): error: method does not override', l)
    if m:
        f = m.group(1).replace('src\\main\\java\\slimeknights\\tconstruct\\library\\recipe\\', '')
        c[f] += 1
        nxt = txt[i+2] if i+2 < len(txt) else ''
        ex[f].append((m.group(2), nxt.strip()[:90]))
for f, n in c.most_common(60):
    print(n, f, ex[f][:2])
