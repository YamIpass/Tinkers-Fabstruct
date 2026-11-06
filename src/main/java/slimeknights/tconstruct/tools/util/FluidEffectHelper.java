package slimeknights.tconstruct.tools.util;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;

/**
 * Helpers for applying fluid effects outside of a transfer transaction.
 * Forge passed a {@code FluidAction} to effect application, Fabric uses the transfer API, where a
 * {@code null} transaction means "simulate only" and an open transaction means "apply the changes".
 */
public final class FluidEffectHelper {
  private FluidEffectHelper() {}

  /** Applies the effect to the entity context, opening a transaction if not simulating */
  public static int applyToEntity(FluidEffects effects, FluidStack fluid, float level, FluidEffectContext.Entity context, boolean simulate) {
    if (simulate) {
      return effects.applyToEntity(fluid, level, context, null);
    }
    try (Transaction transaction = Transaction.openOuter()) {
      int result = effects.applyToEntity(fluid, level, context, transaction);
      transaction.commit();
      return result;
    }
  }

  /** Applies the effect to the block context, opening a transaction if not simulating */
  public static int applyToBlock(FluidEffects effects, FluidStack fluid, float level, FluidEffectContext.Block context, boolean simulate) {
    if (simulate) {
      return effects.applyToBlock(fluid, level, context, null);
    }
    try (Transaction transaction = Transaction.openOuter()) {
      int result = effects.applyToBlock(fluid, level, context, transaction);
      transaction.commit();
      return result;
    }
  }
}
