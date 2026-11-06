import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import patchlib

REL = 'src/main/java/slimeknights/tconstruct/tools/logic/ToolEvents.java'
lines, nl = patchlib.read(REL)
N = chr(10)
text = N.join(lines)

old = '''      if (amount <= 0) {
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT);
      }'''
new = '''      if (amount <= 0) {
        event.setCanceled(true);
      }'''
assert old in text
text = text.replace(old, new)

old = '''  }
  static void onBreakSpeed(PlayerEvents.BreakSpeed event) {'''
new = '''  }

  static void onBreakSpeed(PlayerEvents.BreakSpeed event) {'''
assert old in text
text = text.replace(old, new)

patchlib.write(REL, text.split(N), nl)
print('ok', len(text.split(N)))
