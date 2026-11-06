package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.tconstruct.common.TinkerTags;

/** Fluid Type that does not affect slimes */
public class SlimeFluidType extends TextureFluidType {
  public SlimeFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public boolean canDrownIn(LivingEntity entity) {
    return !entity.getType().is(TinkerTags.EntityTypes.SLIMES);
  }

  public static class Inverted extends SlimeFluidType {
    public Inverted(Properties properties) {
      super(properties);
    }

    // TODO(fabric): Forge's FluidType#initializeClient (ClientInvertedFluidType) has no Porting Lib equivalent.
    //  The upside down fluid texture must instead be registered client side in FluidClientEvents using
    //  slimeknights.mantle.registration.ClientFluidAttributeRegistry#registerUpsideDown.
  }
}
