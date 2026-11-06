"""Classify the files in build/batches/tools.txt: reference availability + Forge API usage."""
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
SRC = os.path.join(ROOT, 'src', 'main', 'java')
BATCH = os.path.join(ROOT, 'build', 'batches', 'tools.txt')
HEPH = os.path.abspath(os.path.join(ROOT, '..', 'ref', 'Hephaestus-1.20.1', 'src', 'main', 'java'))
T364 = os.path.abspath(os.path.join(ROOT, '..', 'ref', 'TConstruct-3.6.4', 'src', 'main', 'java'))

files = []
for line in open(BATCH, encoding='utf-8'):
    m = re.match(r'^tools.(.*?)\s+\((\d+)\)\s*$', line.rstrip())
    if m:
        files.append((m.group(1).replace(os.sep, '/'), int(m.group(2))))

FORGE = re.compile(r'net\.minecraftforge|MinecraftForge|LazyOptional|ForgeCapabilities|ForgeRegistries'
                   r'|ForgeEventFactory|ForgeMod\b|DistExecutor|@OnlyIn|OnlyIn\(|EventBusSubscriber'
                   r'|SubscribeEvent|ItemStackHandler|IItemHandler|NetworkEvent|FMLJavaModLoadingContext'
                   r'|FMLCommonSetupEvent|FMLClientSetupEvent|FMLEnvironment|RegistryObject|GatherDataEvent'
                   r'|IFluidHandler|ItemHandlerHelper|FluidType|ForgeHooks|PlayerInteractEvent|PlayerEvent\.'
                   r'|TickEvent\.|LivingEvent\.')

rows = []
for rel, n in files:
    full_rel = 'tools/' + rel
    p = os.path.join(SRC, 'slimeknights', 'tconstruct', full_rel + '.java')
    try:
        txt = open(p, encoding='utf-8').read()
    except FileNotFoundError:
        rows.append((n, rel, 'MISSING', 0, 0))
        continue
    hits = sorted(set(m.group(0) for m in FORGE.finditer(txt)))
    hp = os.path.join(HEPH, 'slimeknights', 'tconstruct', full_rel + '.java')
    bp = os.path.join(T364, 'slimeknights', 'tconstruct', full_rel + '.java')
    ref = 'H' if os.path.exists(hp) else ('B' if os.path.exists(bp) else '-')
    rows.append((n, rel, ref, len(txt.split('\n')), len(hits)))

rows2 = [(n, 'tools/' + r, ref, l, h) for (n, r, ref, l, h) in rows]
rows.sort(key=lambda r: -r[0])
tot_forge = sum(1 for r in rows if r[4] > 0)
print('files: %d  with-forge-ish: %d  no-forge: %d' % (len(rows), tot_forge, len(rows) - tot_forge))
print('ref: H=hephaestus  B=3.6.4-basename-only  -=none')
for n, rel, ref, lines, hits in rows:
    if hits > 0 or ref == '-':
        print('%4d %s lines=%5d forge=%3d %s' % (n, ref, lines, hits, rel))
