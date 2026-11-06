import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import patchlib

REL = 'src/main/java/slimeknights/tconstruct/tools/logic/ToolEvents.java'
lines, nl = patchlib.read(REL)
N = chr(10)
text = N.join(lines)

def sub(old, new, count=1):
    global text
    n = text.count(old)
    assert n >= count, 'not found (%d): %r' % (n, old[:120])
    text = text.replace(old, new, count)

# ---- imports -------------------------------------------------------------
out = []
for ln in text.split(N):
    if ln.startswith('import net.minecraftforge.'):
        continue
    out.append(ln)
text = N.join(out)

add = [
    'io.github.fabricators_of_create.porting_lib.core.event.BaseEvent.Result',
    'io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents',
    'io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents',
    'io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingTickEvent',
    'io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingVisibilityEvent',
    'io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents',
    'io.github.fabricators_of_create.porting_lib.entity.events.ProjectileImpactEvent',
    'io.github.fabricators_of_create.porting_lib.entity.events.living.LivingDamageEvent',
    'io.github.fabricators_of_create.porting_lib.util.EntityHelper',
    'net.minecraft.server.level.ServerPlayer',
]
existing = set()
for ln in text.split(N):
    if ln.startswith('import '):
        existing.add(ln[len('import '):].rstrip(';').strip())
newimports = [fq for fq in add if fq not in existing
              and fq.rsplit('.', 1)[-1] not in {e.rsplit('.', 1)[-1] for e in existing}]
out = []
inserted = False
for ln in text.split(N):
    if not inserted and ln.startswith('import slimeknights.'):
        for fq in newimports:
            out.append('import %s;' % fq)
        inserted = True
    out.append(ln)
assert inserted, 'no import block'
text = N.join(out)

# ---- drop annotations ----------------------------------------------------
out = []
for ln in text.split(N):
    s = ln.strip()
    if s.startswith('@EventBusSubscriber('):
        continue
    if s.startswith('@SubscribeEvent'):
        continue
    if s.startswith('@SuppressWarnings("removal")'):
        continue
    out.append(ln)
text = N.join(out)

# ---- add init() ----------------------------------------------------------
INIT = [
    '',
    '  public static void init() {',
    '    PlayerEvents.BREAK_SPEED.register(ToolEvents::onBreakSpeed);',
    '    ToolHarvestEvent.EVENT.register(ToolEvents::onHarvest);',
    '    LivingEntityEvents.HURT.register(ToolEvents::livingAttack);',
    '    LivingEntityEvents.HURT.register(ToolEvents::livingHurt);',
    '    LivingDamageEvent.DAMAGE.register(ToolEvents::livingDamage);',
    '    LivingTickEvent.TICK.register(ToolEvents::livingWalk);',
    '    LivingVisibilityEvent.VISIBILITY.register(ToolEvents::livingVisibility);',
    '    EntityEvents.PROJECTILE_IMPACT.register(ToolEvents::projectileHit);',
    '    EntityEvents.START_TRACKING_TAIL.register(ToolEvents::projectileSync);',
    '  }',
]
sub('public class ToolEvents {' + N, 'public class ToolEvents {' + N + N.join(INIT) + N)

# ---- onBreakSpeed --------------------------------------------------------
sub('static void onBreakSpeed(PlayerEvent.BreakSpeed event) {',
    'static void onBreakSpeed(PlayerEvents.BreakSpeed event) {')
sub('    Player player = event.getEntity();', '    Player player = event.getPlayer();')
sub('BlockSideHitListener.getSideHit(event.getEntity())', 'BlockSideHitListener.getSideHit(event.getPlayer())')
sub('BreakSpeedContext.getMiningModifier(event.getEntity())', 'BreakSpeedContext.getMiningModifier(event.getPlayer())')

# ---- livingAttack: becomes a HURT handler --------------------------------
sub('static void livingAttack(LivingAttackEvent event) {' + N + '    LivingEntity entity = event.getEntity();',
    'static float livingAttack(DamageSource source, LivingEntity entity, float amount) {')
sub('    // I cannot think of a reason to run when invulnerable' + N + '    DamageSource source = event.getSource();',
    '    // I cannot think of a reason to run when invulnerable')
sub('''    if (entity.level().isClientSide() || entity.isDeadOrDying()) {
      return;
    }''', '''    if (entity.level().isClientSide() || entity.isDeadOrDying()) {
      return amount;
    }''')
sub('''    if (entity.isInvulnerableTo(source)) {
      return;
    }''', '''    if (entity.isInvulnerableTo(source)) {
      return amount;
    }''')
sub('''    EquipmentContext context = new EquipmentContext(entity);
    float amount = event.getAmount();''', '''    EquipmentContext context = new EquipmentContext(entity);''')
sub('''              event.setCanceled(true);
              return;''', '''              return 0;''')
# close of livingAttack: the block right before getArmorDamage javadoc
sub('''            }
          }
        }
      }
    }
  }

  /**
   * Determines how much to damage armor''', '''            }
          }
        }
      }
    }
    return amount;
  }

  /**
   * Determines how much to damage armor''')

# ---- livingHurt: becomes a HURT handler ----------------------------------
sub('static void livingHurt(LivingHurtEvent event) {' + N + '    LivingEntity entity = event.getEntity();',
    'static float livingHurt(DamageSource source, LivingEntity entity, float originalDamage) {')
sub('''    // determine if there is any modifiable armor, if not nothing to do
    DamageSource source = event.getSource();
    EquipmentContext context = new EquipmentContext(entity);''',
    '''    // determine if there is any modifiable armor, if not nothing to do
    EquipmentContext context = new EquipmentContext(entity);''')
sub('''    float modifierValue = 0;
    float originalDamage = event.getAmount();''', '''    float modifierValue = 0;''')
sub('''    // ensure any changes made so far apply, though we may change it again
    event.setAmount(originalDamage);''', '''    // ensure any changes made so far apply, though we may change it again''')
sub('''      originalDamage = ModifyDamageModifierHook.modifyDamageTaken(ModifierHooks.MODIFY_HURT, context, source, originalDamage, OnAttackedModifierHook.isDirectDamage(source));
      event.setAmount(originalDamage);
      if (originalDamage <= 0) {
        event.setCanceled(true);
        return;
      }''', '''      originalDamage = ModifyDamageModifierHook.modifyDamageTaken(ModifierHooks.MODIFY_HURT, context, source, originalDamage, OnAttackedModifierHook.isDirectDamage(source));
      if (originalDamage <= 0) {
        return 0;
      }''')
sub('''      // set the final dealt damage
      float finalDamage = ArmorUtil.getDamageForEvent(originalDamage, armor, toughness, vanillaModifier, modifierValue, cap);
      event.setAmount(finalDamage);''', '''      // set the final dealt damage
      float uncappedDamage = originalDamage;
      float finalDamage = ArmorUtil.getDamageForEvent(originalDamage, armor, toughness, vanillaModifier, modifierValue, cap);
      originalDamage = finalDamage;''')
sub('int damageMissed = getArmorDamage(originalDamage) - getArmorDamage(finalDamage);',
    'int damageMissed = getArmorDamage(uncappedDamage) - getArmorDamage(finalDamage);')
# end of livingHurt (followed by livingDamage)
sub('''        }
      }
    }
  }

  static void livingDamage(LivingDamageEvent event) {''', '''        }
      }
    }
    return originalDamage;
  }

  static void livingDamage(LivingDamageEvent event) {''')

# ---- livingWalk ----------------------------------------------------------
sub('''    if (!living.isSpectator() && !living.level().isClientSide() && living.isAlive() && !Objects.equals(living.lastPos, pos)) {''',
    '''    BlockPos lastPos = EntityHelper.getLastPos(living);
    if (!living.isSpectator() && !living.level().isClientSide() && living.isAlive() && !Objects.equals(lastPos, pos)) {''')
sub('entry.getHook(ModifierHooks.BOOT_WALK).onWalk(tool, entry, living, living.lastPos, pos);',
    'entry.getHook(ModifierHooks.BOOT_WALK).onWalk(tool, entry, living, lastPos, pos);')

# ---- livingVisibility ----------------------------------------------------
sub('    LivingEntity living = event.getEntity();' + N + '    TinkerDataCapability.Holder data',
    '    LivingEntity living = (LivingEntity) event.getEntity();' + N + '    TinkerDataCapability.Holder data')

# ---- projectileSync ------------------------------------------------------
sub('''  static void projectileSync(PlayerEvent.StartTracking event) {
    Entity entity = event.getTarget();
    if (entity instanceof Projectile) {
      TinkerNetwork.getInstance().sendTo(new SyncProjectileModifiersPacket(entity), event.getEntity());
    }
  }''', '''  static void projectileSync(Entity entity, ServerPlayer player) {
    if (entity instanceof Projectile) {
      TinkerNetwork.getInstance().sendTo(new SyncProjectileModifiersPacket(entity), player);
    }
  }''')

# ---- projectileHit -------------------------------------------------------
text = text.replace('event.setCanceled(true);', 'event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT);')

patchlib.write(REL, text.split(N), nl)
print('written lines:', len(text.split(N)))
for tok in ['net.minecraftforge', '@SubscribeEvent', '@EventBusSubscriber', 'PlayerEvent.', 'lastPos']:
    hits = [i + 1 for i, l in enumerate(text.split(N)) if tok in l]
    print(tok, '->', hits[:8])
