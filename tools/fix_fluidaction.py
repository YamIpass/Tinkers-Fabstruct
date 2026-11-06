"""Replaces Forge FluidAction with a plain `boolean simulate` (per parent ruling 2026-11-05).

SIMULATE -> true, EXECUTE -> false, `FluidAction action` param -> `boolean simulate`.
"""
import sys, os, glob, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import patchlib as P

ROOT = 'src/main/java/slimeknights/tconstruct/tools'
NL = chr(10)
changed = []

for path in sorted(glob.glob(ROOT + '/**/*.java', recursive=True)):
    rel = path.replace(os.sep, '/')
    lines, nl = P.read(rel)
    body = NL.join(lines)
    if 'FluidAction' not in body:
        continue
    has_param = 'FluidAction action' in body
    body = body.replace('FluidAction.SIMULATE', 'true')
    body = body.replace('FluidAction.EXECUTE', 'false')
    body = body.replace('FluidAction action', 'boolean simulate')
    if has_param:
        body = body.replace('action.simulate()', 'simulate')
        body = re.sub(r'(?<![\w.])action(?![\w])', 'simulate', body)
    lines = [l for l in body.split(NL) if l.strip() != 'import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;']
    P.write(rel, lines, nl)
    changed.append(rel)

print('files changed:', len(changed))
for rel in changed:
    print(' ', rel)
