package slimeknights.tconstruct.library.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * This class contains various methods that are safe to call on both sides, which internally call client only code.
 */
public class SafeClient {
  /**
   * Triggers a model update if needed for this tank block
   * @param be          Block entity instance
   * @param tank        Fluid tank instance
   * @param oldAmount   Old fluid amount
   * @param newAmount   New fluid amount
   */
  public static void updateFluidModel(BlockEntity be, FluidTank tank, int oldAmount, int newAmount) {
    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
      ClientOnly.updateFluidModel(be, tank, oldAmount, newAmount);
    }
  }

  /** This class is only ever loaded client side */
  private static class ClientOnly {
    /** @see SafeClient#updateFluidModel(BlockEntity, FluidTank, int, int)  */
    public static void updateFluidModel(BlockEntity be, FluidTank tank, int oldAmount, int newAmount) {
      Level level = be.getLevel();
      if (level != null && level.isClientSide) {
        // if the amount change is bigger than a single increment, or we changed whether we have a fluid, update the world renderer
        BlockState state = be.getBlockState();
        if (oldAmount != newAmount) {
          // TODO(fabric): Forge's BlockEntity#requestModelDataUpdate has no Fabric equivalent, the block change below refreshes the renderer
          Minecraft.getInstance().levelRenderer.blockChanged(level, be.getBlockPos(), state, state, 3);
        }
      }
    }
  }
}
