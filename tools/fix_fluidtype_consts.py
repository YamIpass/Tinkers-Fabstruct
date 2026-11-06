import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
import patchlib

NL = chr(10)
ROOT = 'src/main/java/slimeknights/tconstruct/tools/'

# 1. add the heads tag key to the severing module
rel = 'modules/combat/SeveringModule.java'
lines, nl = patchlib.read(ROOT + rel)
text = NL.join(lines)
anchor = 'public class SeveringModule implements'
idx = text.find(anchor)
assert idx >= 0
open_brace = text.find('{' + NL, idx)
assert open_brace >= 0
field = (NL + '  /** Tag for items that already count as a head drop' + NL
         + '   * TODO(fabric): Forge\'s forge:heads tag data is not ported, this key is empty until a datapack provides it */' + NL
         + '  private static final TagKey<Item> HEADS = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "heads"));' + NL)
text = text[:open_brace + len('{' + NL)] + field + text[open_brace + len('{' + NL):]
patchlib.write(ROOT + rel, text.split(NL), nl)

# 2. Forge's FluidType.BUCKET_VOLUME is not on the Fabric type, inline its value
count = 0
for dp, _, fs in os.walk(ROOT):
  for f in fs:
    if not f.endswith('.java'):
      continue
    p = os.path.join(dp, f)
    rel = p[len(ROOT):]
    lines, nl = patchlib.read(ROOT + rel)
    text = NL.join(lines)
    n = text.count('FluidType.BUCKET_VOLUME')
    if n:
      text = text.replace('FluidType.BUCKET_VOLUME', '1000')
      if 'FluidType' not in text.replace('import io.github.fabricators_of_create.porting_lib.fluids.FluidType;', ''):
        lines = patchlib.rm_imports(text.split(NL), ['io.github.fabricators_of_create.porting_lib.fluids.FluidType'])
      else:
        lines = text.split(NL)
      patchlib.write(ROOT + rel, lines, nl)
      count += n
      print('  bucket volume', rel, n)
print('inlined', count, 'bucket volume constants')
