"""Build build/jarindex.txt: fully qualified class names of every class in libs/*.jar."""
import os
import subprocess
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
LIBS = os.path.join(ROOT, 'libs')
OUT = os.path.join(ROOT, 'build', 'jarindex.txt')

entries = set()
for jar in sorted(os.listdir(LIBS)):
    if not jar.endswith('.jar'):
        continue
    path = os.path.join(LIBS, jar)
    with zipfile.ZipFile(path) as zf:
        for name in zf.namelist():
            if name.endswith('.class'):
                entries.add(name[:-6].replace('/', '.'))

with open(OUT, 'w', encoding='utf-8') as f:
    f.write('\n'.join(sorted(entries)))
print('entries:', len(entries))
print('check:', 'slimeknights.mantle.data.loadable.record.RecordLoadable' in entries)
