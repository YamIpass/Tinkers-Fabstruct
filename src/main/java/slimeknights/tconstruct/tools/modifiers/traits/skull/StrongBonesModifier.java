package slimeknights.tconstruct.tools.modifiers.traits.skull;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityUseItemEvents;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorLevelModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.CureOnRemovalModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;

// TODO: make JSON?
public class StrongBonesModifier extends SingleLevelModifier {
  /** Key for modifiers that are boosted by drinking milk */
  public static final TinkerDataKey<Integer> CALCIFIABLE = TConstruct.createKey("calcifable");
  /** Module to add to any calcifiable modifiers */
  public static final ArmorLevelModule CALCIFIABLE_MODULE = new ArmorLevelModule(CALCIFIABLE, false, TinkerTags.Items.HELD_ARMOR);

  public StrongBonesModifier() {
    // TODO: move this out of constructor to generalized logic
    LivingEntityUseItemEvents.LIVING_USE_ITEM_FINISH.register((entity, stack, remaining, result) -> {
      onItemFinishUse(entity, result);
      return result;
    });
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(CureOnRemovalModule.HELMET);
  }

  private static boolean drinkMilk(LivingEntity living, int flat, int eachLevel, boolean simulate) {
    // strong bones has to be the helmet as we use it for curing
    // TODO 1.20: can use the new cure effects to make this work in any slot
    ItemStack helmet = living.getItemBySlot(EquipmentSlot.HEAD);
    boolean didSomething = false;
    int level = ModifierUtil.getModifierLevel(helmet, TinkerModifiers.strongBones.getId());
    if (level > 0) {
      MobEffectInstance effect = new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, flat + eachLevel * level);
      effect.getCurativeItems().clear();
      effect.getCurativeItems().add(new ItemStack(helmet.getItem()));
      // on simulate, don't apply the effect, just ask if we can apply
      didSomething = !simulate ? living.addEffect(effect) : living.canBeAffected(effect);
      // quick exit on simulate: no more information needed
      if (didSomething && simulate) {
        return true;
      }
    }
    level = ArmorLevelModule.getLevel(living, CALCIFIABLE);
    if (level > 0) {
      MobEffectInstance effect = new MobEffectInstance(TinkerModifiers.calcifiedEffect.get(), flat + eachLevel * level, 0);
      didSomething |= !simulate ? living.addEffect(effect) : living.canBeAffected(effect);
    }
    return didSomething;
  }

  /** Called when you finish drinking milk */
  private static void onItemFinishUse(LivingEntity living, ItemStack item) {
    if (item.getItem() == Items.MILK_BUCKET) {
      drinkMilk(living, 600, 600, false);
    }
  }


  /* Spilling effect */

  /** Singleton instance spilling effect */
  public static final FluidEffect<FluidEffectContext.Entity> FLUID_EFFECT = FluidEffect.simple((fluid, scale, context, transaction) -> {
    LivingEntity target = context.getLivingTarget();
    // while we could scale, doing it flat ensures we don't charge extra
    if (target != null && drinkMilk(target, 0, (int)(20*10 * scale.value()), transaction == null)) {
      return scale.value();
    }
    return 0;
  });
}
