import re, sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
import patchlib

NL = chr(10)
P = 'build/resolved/tools/logic/InteractionHandler.java'
lines, nl = patchlib.read(P)
text = NL.join(lines)


def J(*parts):
  return NL.join(parts)


# 1. hook holder rename
text = text.replace('TinkerHooks.ENTITY_INTERACT', 'ModifierHooks.ENTITY_INTERACT')
text = text.replace('TinkerHooks.BLOCK_INTERACT', 'ModifierHooks.BLOCK_INTERACT')

# 2. 3.12.1 tool attack API (reference used the 3.6.4 seven-argument form)
old_attack = '          ToolAttackUtil.attackEntity(tool, attacker, InteractionHand.MAIN_HAND, target, ToolAttackUtil.getCooldownFunction(attacker, InteractionHand.MAIN_HAND), false, EquipmentSlot.CHEST);'
new_attack = J('          if (ToolAttackUtil.isAttackable(attacker, target)) {',
               '            ToolAttackUtil.performAttack(tool, ToolAttackContext.attacker(attacker).target(target).slot(EquipmentSlot.CHEST, InteractionHand.MAIN_HAND).defaultCooldown().toolAttributes(tool).build());',
               '          }')
assert old_attack in text, 'attack line'
text = text.replace(old_attack, new_attack)

# 3. strip stale Forge remnants left commented out by the merge
for stale in ['        /*if (event.getUseItem() != Result.DENY)*/ {',
              '//        Result useBlock = event.getUseBlock();',
              '//        Result useItem = event.getUseItem();',
              '//        event.setCancellationResult(InteractionResult.PASS);',
              '//        event.setCanceled(true);']:
  text = text.replace(stale + NL, '')

# 4. restore the 3.12.1 sneak-bypass condition
new_cond = J('        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;',
             '        if (!player.isSecondaryUseActive() || player.getItemInHand(otherHand).doesSneakBypassUse(player.level(), pos, player)) {')
text, n1 = re.subn(r' *if \(/\*useBlock == Result\.ALLOW.*?\)\)\) \{', lambda m: new_cond, text, flags=re.S)
assert n1 == 1, n1

text, n2 = re.subn(r' *if \(/\*useItem != Result\.DENY.*?\(chestplate\.getItem\(\)\)\)\) \{',
                   '        if (!player.getCooldowns().isOnCooldown(chestplate.getItem())) {', text)
assert n2 == 1, n2

# 5. fix the mangled loop indentation in beforeEntityInteract
old_loop = J('      for (ModifierEntry entry : tool.getModifierList()) {',
             '      // exit on first successful result',
             '      InteractionResult result = entry.getHook(ModifierHooks.ENTITY_INTERACT).beforeEntityUse(tool, entry, player, target, hand, source);',
             '      if (result.consumesAction()) {',
             '        return result;',
             '        }',
             '      }')
new_loop = J('      for (ModifierEntry entry : tool.getModifierList()) {',
             '        // exit on first successful result',
             '        InteractionResult result = entry.getHook(ModifierHooks.ENTITY_INTERACT).beforeEntityUse(tool, entry, player, target, hand, source);',
             '        if (result.consumesAction()) {',
             '          return result;',
             '        }',
             '      }')
assert old_loop in text, 'loop'
text = text.replace(old_loop, new_loop)

lines = text.split(NL)
if any(l.startswith('import ') for l in lines):
  lines = patchlib.add_imports(lines, ['slimeknights.tconstruct.library.tools.context.ToolAttackContext'])
patchlib.write(P, lines, nl)
print('patched', P, len(lines), 'lines')
for i, l in enumerate(lines, 1):
  if 'TinkerHooks' in l or 'event.' in l or 'Result.' in l or 'attackEntity(' in l or 'performAttack(' in l:
    print(i, l.rstrip())
