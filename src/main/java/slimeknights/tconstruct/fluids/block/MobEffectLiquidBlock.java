package slimeknights.tconstruct.fluids.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/** @deprecated use {@link slimeknights.mantle.block.fluid.MobEffectLiquidBlock} */
@Deprecated(forRemoval = true)
public class MobEffectLiquidBlock extends LiquidBlock {
  private final Supplier<MobEffectInstance> effect;
  public MobEffectLiquidBlock(Supplier<? extends FlowingFluid> supplier, Properties properties, Supplier<MobEffectInstance> effect) {
    super(supplier.get(), properties);
    this.effect = effect;
  }

  @Deprecated
  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    // TODO(fabric): Forge queried the fluid type height of the entity, Fabric only knows the fluid state at its position
    if (level.getFluidState(entity.blockPosition()).getType().isSame(this.fluid) && entity instanceof LivingEntity living) {
      MobEffectInstance effect = this.effect.get();
      effect.setCurativeItems(new ArrayList<>());
      living.addEffect(effect);
    }
  }

  /** Creates a new block supplier */
  public static Function<Supplier<? extends FlowingFluid>, LiquidBlock> createEffect(MapColor color, int lightLevel, Supplier<MobEffectInstance> effect) {
    return fluid -> new MobEffectLiquidBlock(fluid, FluidDeferredRegister.createProperties(color, lightLevel), effect);
  }
}
