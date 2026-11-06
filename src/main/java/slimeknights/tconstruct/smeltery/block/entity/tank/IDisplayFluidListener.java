package slimeknights.tconstruct.smeltery.block.entity.tank;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import slimeknights.mantle.client.model.ModelProperty;

/**
 * Interface for blocks to be notified when the smeltery has a new bottommost fluid
 */
@SuppressWarnings("removal")
public interface IDisplayFluidListener {
  /** Model property for the displayed fluid */
  // TODO(fabric): 3.12.1 removed this in favor of the master notifying listeners; kept as rendering glue
  ModelProperty<FluidStack> PROPERTY = new ModelProperty<>();

  /**
   * Called when the display fluid changes0
   * @param fluid New display fluid, is safe to store (will not be modified)
   */
  void notifyDisplayFluidUpdated(FluidStack fluid);

  /** Makes the fluid contain 1000mb, or {@link FluidStack#EMPTY} if empty */
  static FluidStack normalizeFluid(FluidStack fluid) {
    if (fluid.isEmpty()) {
      return FluidStack.EMPTY;
    }
    fluid = fluid.copy();
    fluid.setAmount(FluidConstants.BUCKET);
    return fluid;
  }
}
