import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
import patchlib

NL = chr(10)
ROOT = 'src/main/java/slimeknights/tconstruct/tools/'
fails = []
done = []


def patch(rel, edits, drop_imports=(), add_imports=()):
  try:
    lines, nl = patchlib.read(ROOT + rel)
    text = NL.join(lines)
    for old, new in edits:
      if old not in text:
        raise AssertionError('missing: ' + old[:90])
      text = text.replace(old, new)
    lines = text.split(NL)
    if drop_imports:
      lines = patchlib.rm_imports(lines, list(drop_imports))
    if add_imports:
      lines = patchlib.add_imports(lines, list(add_imports))
    patchlib.write(ROOT + rel, lines, nl)
    done.append(rel)
  except Exception as e:
    fails.append((rel, str(e)))


# 1. tier sorting moved to Porting Lib
patch('modules/MeltingFluidEffectiveModule.java', [],
      drop_imports=['net.minecraftforge.common.TierSortingRegistry'],
      add_imports=['io.github.fabricators_of_create.porting_lib.util.TierSortingRegistry'])

# 2. fluid sound API lives in Porting Lib, fluid type via interface injection
patch('modules/interaction/BucketModule.java',
      [('pickedUpFluid.getFluidType()', '((FluidExtension) pickedUpFluid).getFluidType()')],
      drop_imports=['net.minecraftforge.common.SoundActions'],
      add_imports=['io.github.fabricators_of_create.porting_lib.fluids.sound.SoundActions',
                   'io.github.fabricators_of_create.porting_lib.fluids.extensions.FluidExtension'])

# 3. shearable hook moved to Porting Lib extensions
patch('modules/interaction/ShearsModule.java',
      [('instanceof IForgeShearable target', 'instanceof IShearable target')],
      drop_imports=['net.minecraftforge.common.IForgeShearable'],
      add_imports=['io.github.fabricators_of_create.porting_lib.extensions.extensions.IShearable'])

# 4. projectile impact event instead of the Forge factory hook
patch('entity/ThrownShuriken.java',
      [('      if (!ForgeEventFactory.onProjectileImpact(this, hit)) {' + NL
        + '        this.onHit(hit);' + NL
        + '      }',
        '      ProjectileImpactEvent impactEvent = new ProjectileImpactEvent(this, hit);' + NL
        + '      impactEvent.sendEvent();' + NL
        + '      // Porting Lib reports the cancellation as an impact result, matching Forge cancel semantics' + NL
        + '      if (impactEvent.getImpactResult() != ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE) {' + NL
        + '        this.onHit(hit);' + NL
        + '      }')],
      drop_imports=['net.minecraftforge.event.ForgeEventFactory'],
      add_imports=['io.github.fabricators_of_create.porting_lib.entity.events.ProjectileImpactEvent'])

# 5. packets use the Mantle packet context
for packet in ['network/TinkerControlPacket.java', 'network/ToolContainerFluidUpdatePacket.java',
               'network/SyncProjectileModifiersPacket.java', 'network/PushBlockRowPacket.java']:
  patch(packet, [], drop_imports=['net.minecraftforge.network.NetworkEvent.Context'],
        add_imports=['slimeknights.mantle.network.packet.ISimplePacket.Context'])

# 6. item registry iteration
patch('recipe/EnchantmentConvertingRecipe.java',
      [('ForgeRegistries.ITEMS.getValues()', 'BuiltInRegistries.ITEM')],
      drop_imports=['net.minecraftforge.registries.ForgeRegistries'],
      add_imports=['net.minecraft.core.registries.BuiltInRegistries'])

# 7. leftover Forge tag import
patch('recipe/ArmorDyeingRecipe.java', [], drop_imports=['net.minecraftforge.common.Tags.Items'])

# 8. CCA lookup instead of capability holders
patch('modules/ZoomModule.java',
      [('    living.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).set(modifier.getId(), amount));',
        '    TinkerDataCapability.CAPABILITY.maybeGet(living).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).set(modifier.getId(), amount));'),
       ('  private static void stopZoom(ModifierEntry modifier, LazyOptional<TinkerDataCapability.Holder> tinkerData) {' + NL
        + '    tinkerData.ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).remove(modifier.getId()));' + NL
        + '  }',
        '  private static void stopZoom(ModifierEntry modifier, TinkerDataCapability.Holder tinkerData) {' + NL
        + '    tinkerData.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).remove(modifier.getId());' + NL
        + '  }'),
       ('    stopZoom(modifier, entity.getCapability(TinkerDataCapability.CAPABILITY));',
        '    TinkerDataCapability.CAPABILITY.maybeGet(entity).ifPresent(data -> stopZoom(modifier, data));')],
      drop_imports=['net.minecraftforge.common.util.LazyOptional'])

# 9. knockback direction cannot be observed from the Porting Lib event yet
patch('modules/ranged/RestrictAngleModule.java',
      [('  public static void onKnockback(LivingKnockBackEvent event, int level) {' + NL
        + '    // start at 4 directions at level 1, then 32, 16, 8, and 4 by level 4, don\'t go below 4 directions' + NL
        + '    Vec3 direction = clampDirection(new Vec3(event.getRatioX(), 0, event.getRatioZ()), level, null);' + NL
        + '    event.setRatioX(direction.x);' + NL
        + '    event.setRatioZ(direction.z);' + NL
        + '  }',
        '  public static Vec3 clampKnockback(Vec3 knockback, int level) {' + NL
        + '    // start at 4 directions at level 1, then 32, 16, 8, and 4 by level 4, don\'t go below 4 directions' + NL
        + '    // TODO(fabric): Porting Lib\'s knockback event exposes no direction, this needs a mixin on LivingEntity.knockback(double,double,double)' + NL
        + '    return clampDirection(new Vec3(knockback.x, 0, knockback.z), level, null);' + NL
        + '  }')],
      drop_imports=['net.minecraftforge.event.entity.living.LivingKnockBackEvent'])

# 10. teleport events are fired through the Porting Lib event bus helper
patch('modules/interaction/sling/SlingTeleportModule.java',
      [('            MinecraftForge.EVENT_BUS.post(event);', '            event.sendEvent();')],
      drop_imports=['net.minecraftforge.common.MinecraftForge'])

# 11. break speed event moved to Porting Lib
for mod in ['modifiers/traits/harvest/TemperateModifier.java', 'modifiers/traits/harvest/MomentumModifier.java',
            'modifiers/traits/harvest/DwarvenModifier.java']:
  patch(mod, [], drop_imports=['net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed'],
        add_imports=['io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents.BreakSpeed'])

# 12. no forge:heads tag on Fabric, keep a local key so datapacks can still provide it
patch('modules/combat/SeveringModule.java',
      [('      if (generatedLoot.stream().noneMatch(stack -> stack.is(Tags.Items.HEADS))) {',
        '      if (generatedLoot.stream().noneMatch(stack -> stack.is(HEADS))) {')],
      drop_imports=['net.minecraftforge.common.Tags'],
      add_imports=['net.minecraft.core.registries.Registries', 'net.minecraft.resources.ResourceLocation',
                   'net.minecraft.tags.TagKey', 'net.minecraft.world.item.Item'])

print('OK ' + str(len(done)))
for d in done:
  print('  done', d)
print('FAIL ' + str(len(fails)))
for f, e in fails:
  print('  fail', f, '->', e)
