package slimeknights.tconstruct.fluids.util;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.world.item.ItemStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

/** @deprecated use {@link slimeknights.mantle.item.ConstantFluidContainerWrapper} */
@Deprecated(forRemoval = true)
public class ConstantFluidContainerWrapper extends slimeknights.mantle.item.ConstantFluidContainerWrapper {
  public ConstantFluidContainerWrapper(FluidStack fluid, ItemStack container) {
    super(fluid, ContainerItemContext.withConstant(container));
  }

  public ConstantFluidContainerWrapper(FluidStack fluid, ItemStack container, ItemStack emptyStack) {
    super(fluid, ContainerItemContext.withConstant(container), emptyStack);
  }
}
