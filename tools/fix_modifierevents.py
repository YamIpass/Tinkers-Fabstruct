"""Ports tools/logic/ModifierEvents.java to Porting Lib / Fabric events."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import patchlib as P

REL = 'src/main/java/slimeknights/tconstruct/tools/logic/ModifierEvents.java'

lines, nl = P.read(REL)

# ---- imports -------------------------------------------------------------
old_imports = """import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent.ImpactResult;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import slimeknights.mantle.MantleEvents;
""".split('\n')[:-1]
assert lines[len([l for l in lines if l.startswith('import ')]) - 1:], 'sanity'
for imp in old_imports:
    assert imp in lines, 'missing ' + imp
idx = lines.index(old_imports[0])
assert lines[idx:idx + len(old_imports)] == old_imports, 'import block not contiguous'
new_imports = """import io.github.fabricators_of_create.porting_lib.attributes.PortingLibAttributes;
import io.github.fabricators_of_create.porting_lib.core.event.BaseEvent.Result;
import io.github.fabricators_of_create.porting_lib.entity.events.CriticalHitEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingDeathEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.Fall.FallEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingJumpEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingTickEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.ProjectileImpactEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.ProjectileImpactEvent.ImpactResult;
import io.github.fabricators_of_create.porting_lib.entity.events.living.MobEffectEvent;
import io.github.fabricators_of_create.porting_lib.event.common.BlockEvents;
import net.fabricmc.fabric.api.entity.FakePlayer;""".split('\n')
lines[idx:idx + len(old_imports)] = new_imports
P.add_imports(lines, ['net.minecraft.world.entity.item.ItemEntity', 'java.util.Collection'])

txt = '\n'.join(lines)

# ---- class annotation ---------------------------------------------------
txt = txt.replace("@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Bus.FORGE)\npublic class ModifierEvents {",
                  "public class ModifierEvents {", 1)
assert '@EventBusSubscriber' not in txt

# ---- soulbound slot key (Mantle 1.11 dropped MantleEvents) ---------------
txt = txt.replace("""  /** Volatile data int for making a modifier on a shield grant reflecting */
  public static final ResourceLocation REFLECTING = TConstruct.getResource("reflecting");
""", """  /** Volatile data int for making a modifier on a shield grant reflecting */
  public static final ResourceLocation REFLECTING = TConstruct.getResource("reflecting");
  /** Persistent data key for the slot the item was in when the player died. Matches Mantle's constant, which Fabric Mantle dropped */
  private static final ResourceLocation SOULBOUND_SLOT = new ResourceLocation("mantle", "soulbound_slot");

  /** Registers all the event handlers in this class */
  public static void init() {
    LivingEntityEvents.KNOCKBACK_STRENGTH.register(ModifierEvents::onKnockback);
    LivingEntityEvents.FALL.register(ModifierEvents::onLivingFall);
    LivingEntityEvents.FALL.register(ModifierEvents::bounceOnFall);
    LivingEntityEvents.LivingJumpEvent.JUMP.register(ModifierEvents::onLivingJump);
    MobEffectEvent.APPLICABLE.register(ModifierEvents::isPotionApplicable);
    MobEffectEvent.ADDED.register(ModifierEvents::onPotionStart);
    LivingEntityEvents.EXPERIENCE_DROP.register(ModifierEvents::onExperienceDrop);
    LivingEntityEvents.DROPS.register(ModifierEvents::onLivingDrops);
    LivingDeathEvent.DEATH.register(ModifierEvents::onLivingDeath);
    BlockEvents.BLOCK_BREAK.register(ModifierEvents::beforeBlockBreak);
    CriticalHitEvent.CRITICAL_HIT.register(ModifierEvents::onCritical);
    EntityEvents.PROJECTILE_IMPACT.register(ModifierEvents::projectileImpact);
    LivingTickEvent.TICK.register(ModifierEvents::onLivingTick);
  }
""", 1)
txt = txt.replace('MantleEvents.SOULBOUND_SLOT', 'SOULBOUND_SLOT')
assert 'MantleEvents' not in txt

lines = txt.split('\n')

# ---- onKnockback --------------------------------------------------------
s, e = P.method_bounds(lines, 'static void onKnockback(')
lines[s:e + 1] = """  /** Called on knockback to adjust the knockback strength */
  static double onKnockback(double strength, Player player) {
    Optional<TinkerDataCapability.Holder> dataCap = player.getCapability(TinkerDataCapability.CAPABILITY).resolve();
    double knockback = player.getAttributeValue(TinkerAttributes.KNOCKBACK_MULTIPLIER.get())
                     + dataCap.map(data -> data.get(TinkerDataKeys.KNOCKBACK)).orElse(0f);
    if (knockback != 1) {
      strength *= knockback;
    }
    // TODO(fabric): Porting Lib's KNOCKBACK_STRENGTH callback exposes no knockback direction, so crystalstrike's
    //  direction clamping (RestrictAngleModule.onKnockback) cannot run from this event; port it with a mixin
    return strength;
  }""".split('\n')

# ---- living fall / bounce ------------------------------------------------
txt = '\n'.join(lines)
txt = txt.replace('static void onLivingFall(LivingFallEvent event) {', 'static void onLivingFall(FallEvent event) {', 1)
txt = txt.replace('static void bounceOnFall(LivingFallEvent event) {', 'static void bounceOnFall(FallEvent event) {', 1)
txt = txt.replace('living.getAttributeValue(ForgeMod.STEP_HEIGHT_ADDITION.get())', 'living.getAttributeValue(PortingLibAttributes.STEP_HEIGHT_ADDITION)')
txt = txt.replace('living.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get())', 'living.getAttributeValue(PortingLibAttributes.ENTITY_GRAVITY)')
assert 'ForgeMod' not in txt

# ---- living drops -------------------------------------------------------
txt = txt.replace("""  static void onLivingDrops(LivingDropsEvent event) {
    DamageSource source = event.getSource();
    if (source != null) {
      float gold = (float) event.getEntity().getAttributeValue(TinkerAttributes.CHRYSOPHILITE.get());""",
"""  static boolean onLivingDrops(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
    if (source != null) {
      float gold = (float) entity.getAttributeValue(TinkerAttributes.CHRYSOPHILITE.get());""", 1)
txt = txt.replace('LivingEntity target = event.getEntity();', 'LivingEntity target = entity;', 1)
txt = txt.replace('event.getDrops().add(target.spawnAtLocation(stack));', 'drops.add(target.spawnAtLocation(stack));', 1)
txt = txt.replace("""          }
        }
      }
    }
  }

  /** Called when the player dies to store the item in the original inventory */""",
"""          }
        }
      }
    }
    return true;
  }

  /** Called when the player dies to store the item in the original inventory */""", 1)

# ---- experience drop ----------------------------------------------------
txt = txt.replace("""  static void onExperienceDrop(LivingExperienceDropEvent event) {
    // boost entity experience if they are under the effects of experienced
    LivingEntity entity = event.getEntity();
    MobEffectInstance instance""",
"""  static int onExperienceDrop(int original, Player player, LivingEntity entity) {
    // boost entity experience if they are under the effects of experienced
    MobEffectInstance instance""", 1)
txt = txt.replace("""    // always add armor boost, unfortunately no good way to stop shield stuff here
    Player player = event.getAttackingPlayer();
    if (player != null) {""",
"""    // always add armor boost, unfortunately no good way to stop shield stuff here
    if (player != null) {""", 1)
txt = txt.replace("    event.setDroppedExperience((int) (event.getDroppedExperience() * multiplier));", "    return (int) (original * multiplier);", 1)

# ---- block break --------------------------------------------------------
txt = txt.replace('static void beforeBlockBreak(BreakEvent event) {', 'static void beforeBlockBreak(BlockEvents.BreakEvent event) {', 1)

lines = txt.split('\n')

# ---- events with no equivalent: comment out and mark -------------------
for needle, why in [
    ('static void onProjectile(', 'Forge\'s LivingGetProjectileEvent has no Fabric or Porting Lib equivalent, so the held ballista ammo swapping cannot run'),
    ('static void onTeleport(', 'Forge\'s EntityTeleportEvent has no Fabric or Porting Lib listener equivalent, so enderference cannot cancel teleports'),
]:
    s, e = P.method_bounds(lines, needle)
    block = lines[s:e + 1]
    lines[s:e + 1] = ['  // TODO(fabric): ' + why + '; restore via a mixin when one is available', '  //' ] + ['  // ' + l.strip() if l.strip() else '  //' for l in block]

# ---- drop the Forge annotations ----------------------------------------
out = []
for l in lines:
    st = l.strip()
    if st == '@SubscribeEvent' or st.startswith('@SubscribeEvent('):
        continue
    if st == '@SuppressWarnings("removal")' or st == '// lets us work with Neo 1.20 for now':
        continue
    out.append(l)
lines = out

P.write(REL, lines, nl)
print('wrote', REL, len(lines), 'lines')
