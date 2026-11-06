package slimeknights.tconstruct.library.modifiers.fluid.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.PotionHelper;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;

/**
 * Effect to clear all effects using the given stack
 * @param stack  Stack used for curing, standard is milk bucket
 */
public record CureEffectsFluidEffect(ItemStack stack) implements FluidEffect<FluidEffectContext.Entity> {
  public static final RecordLoadable<CureEffectsFluidEffect> LOADER = RecordLoadable.create(ItemStackLoadable.REQUIRED_ITEM.requiredField("item", e -> e.stack), CureEffectsFluidEffect::new);

  public CureEffectsFluidEffect(ItemLike item) {
    this(new ItemStack(item));
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, Entity context, TransactionContext transaction) {
    LivingEntity target = context.getLivingTarget();
    if (target != null && level.isFull()) {
      // when simulating, search the effects list directly for curative effects
      // may still be wrong if the event cancels things though, no way to safely simulate it
      if (transaction == null) {
        return target.getActiveEffects().stream().anyMatch(effect -> effect.isCurativeItem(stack)) ? 1 : 0;
      }
      // Porting Lib supplies Forge's LivingEntity#curePotionEffects(ItemStack) as a static helper
      return PotionHelper.curePotionEffects(target, stack) ? 1 : 0;
    }
    return 0;
  }

  @Override
  public RecordLoadable<CureEffectsFluidEffect> getLoader() {
    return LOADER;
  }
}
