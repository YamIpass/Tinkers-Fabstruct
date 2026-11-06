package slimeknights.tconstruct.library.modifiers.fluid.general;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.utils.Util;

/**
 * Fluid effect with a load condition for datagen, condition is evaluated at parse.
 * @param ifTrue      Module to use if all conditions are true.
 * @param ifFalse     Module to use if any condition is false. Defaults to {@link FluidEffect#EMPTY}
 * @param registry    Fluid effect registry.
 * @param conditions  Conditions to evaluate.
 * @param <C> Context type
 */
@SuppressWarnings("unused") // API
public record LoadConditionFluidEffect<C extends FluidEffectContext>(FluidEffect<? super C> ifTrue, FluidEffect<? super C> ifFalse, GenericLoaderRegistry<FluidEffect<? super C>> registry, ConditionJsonProvider... conditions) implements FluidEffect<C> {
  /** Creates a new load condition block effect */
  public static LoadConditionFluidEffect<FluidEffectContext.Block> block(FluidEffect<? super FluidEffectContext.Block> ifTrue, FluidEffect<? super FluidEffectContext.Block> ifFalse, ConditionJsonProvider... conditions) {
    return new LoadConditionFluidEffect<>(ifTrue, ifFalse, FluidEffect.BLOCK_EFFECTS, conditions);
  }

  /** Creates a new load condition block effect */
  public static LoadConditionFluidEffect<FluidEffectContext.Block> block(FluidEffect<? super FluidEffectContext.Block> ifTrue, ConditionJsonProvider... conditions) {
    return block(ifTrue, FluidEffect.EMPTY, conditions);
  }

  /** Creates a new load condition entity effect */
  public static LoadConditionFluidEffect<FluidEffectContext.Entity> entity(FluidEffect<? super FluidEffectContext.Entity> ifTrue, FluidEffect<? super FluidEffectContext.Entity> ifFalse, ConditionJsonProvider... conditions) {
    return new LoadConditionFluidEffect<>(ifTrue, ifFalse, FluidEffect.ENTITY_EFFECTS, conditions);
  }

  /** Creates a new load condition entity effect */
  public static LoadConditionFluidEffect<FluidEffectContext.Entity> entity(FluidEffect<? super FluidEffectContext.Entity> ifTrue, ConditionJsonProvider... conditions) {
    return entity(ifTrue, FluidEffect.EMPTY, conditions);
  }

  @SuppressWarnings("unchecked")
  @Override
  public RecordLoadable<? extends FluidEffect<C>> getLoader() {
    return (RecordLoadable<? extends FluidEffect<C>>)(RecordLoadable<?>) registry.getConditionalLoader();
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, C context, TransactionContext transaction) {
    FluidEffect<? super C> effect = (Util.testConditions(conditions) ? ifTrue : ifFalse);
    return effect.apply(fluid, level, context, transaction);
  }
}
