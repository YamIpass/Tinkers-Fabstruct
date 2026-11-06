import os, re, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
import patchlib

NL = chr(10)
ROOT = 'src/main/java/slimeknights/tconstruct/tools/'
HELPER = 'slimeknights.tconstruct.tools.util.FluidEffectHelper'
fails, done = [], []


def patch(rel, edits, drop=(), add=(), count_check=None):
  try:
    lines, nl = patchlib.read(ROOT + rel)
    text = NL.join(lines)
    for old, new in edits:
      if old not in text:
        raise AssertionError('missing: ' + old[:80])
      text = text.replace(old, new)
    lines = text.split(NL)
    if drop:
      lines = patchlib.rm_imports(lines, list(drop))
    if add:
      lines = patchlib.add_imports(lines, list(add))
    patchlib.write(ROOT + rel, lines, nl)
    done.append(rel)
  except Exception as e:
    fails.append((rel, str(e)))


def route(rel, calls, drop=(), add=(HELPER,)):
  """Rewrites recipe.applyToX(...) calls into FluidEffectHelper.applyToX(recipe, ...)"""
  edits = []
  for call in calls:
    edits.append(('.' + call + '(', '.' + call + '_HELPER('))
  try:
    lines, nl = patchlib.read(ROOT + rel)
    text = NL.join(lines)
    for call in calls:
      # both `recipe.applyToEntity(` and `effects.applyToBlock(` styles
      text = re.sub(r'(\b[A-Za-z_][A-Za-z0-9_]*)\.' + call + r'\(', 'FluidEffectHelper.' + call + r'(\1, ', text)
    if 'FluidEffectHelper.' not in text:
      raise AssertionError('no call rewritten')
    lines = text.split(NL)
    if drop:
      lines = patchlib.rm_imports(lines, list(drop))
    lines = patchlib.add_imports(lines, list(add))
    patchlib.write(ROOT + rel, lines, nl)
    done.append(rel)
  except Exception as e:
    fails.append((rel, str(e)))


route('entity/FluidEffectProjectile.java', ['applyToEntity', 'applyToBlock'])
route('modifiers/ability/fluid/UseFluidOnHitModifier.java', ['applyToEntity'])
route('modules/combat/SpillingModule.java', ['applyToEntity'])
route('modules/interaction/SplashingModule.java', ['applyToEntity', 'applyToBlock'])
route('modules/ranged/ammo/SmashingModule.java', ['applyToEntity', 'applyToBlock'])
route('modules/interaction/SlurpingModule.java', ['applyToEntity'])

# the hook parameter is a boolean now, not the Forge enum
patch('modules/ranged/ammo/SmashingModule.java',
      [('    // success! we can fill' + NL + '    if (simulate.execute()) {',
        '    // success! we can fill' + NL + '    if (!simulate) {'),
       ('          FluidStack result = new FluidStack(fluid, amount, getFluidTag(data));' + NL + '          if (simulate.execute()) {',
        '          FluidStack result = new FluidStack(fluid, amount, getFluidTag(data));' + NL + '          if (!simulate) {'),
       ('            FluidStack result = new FluidStack(fluid, amount, storedTag);' + NL + '            if (simulate.execute()) {',
        '            FluidStack result = new FluidStack(fluid, amount, storedTag);' + NL + '            if (!simulate) {')])

# the fluid effect callback receives a transaction, null means simulate
patch('modifiers/traits/skull/StrongBonesModifier.java',
      [('  public static final FluidEffect<FluidEffectContext.Entity> FLUID_EFFECT = FluidEffect.simple((fluid, scale, context, simulate) -> {',
        '  public static final FluidEffect<FluidEffectContext.Entity> FLUID_EFFECT = FluidEffect.simple((fluid, scale, context, transaction) -> {'),
       ('    if (target != null && drinkMilk(target, 0, (int)(20*10 * scale.value()), simulate)) {',
        '    if (target != null && drinkMilk(target, 0, (int)(20*10 * scale.value()), transaction == null)) {')],
      add=['net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext'])

print('OK', len(done))
for d in done:
  print('  done', d)
print('FAIL', len(fails))
for f, e in fails:
  print('  fail', f, '->', e)
