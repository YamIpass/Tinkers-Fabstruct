package slimeknights.tconstruct.smeltery.block.entity.inventory;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import java.lang.ref.WeakReference;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.library.recipe.fuel.IFluidContainer;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;

/**
 * Fluid tank wrapper that weakly references a tank from a neighbor
 */
public class MelterFuelWrapper implements IFluidContainer {
  // TODO(fabric): Forge's IFluidTank has no Fabric equivalent; the Porting Lib tank covers the same API
  private final WeakReference<FluidTank> tank;

  public MelterFuelWrapper(FluidTank tank) {
    this.tank = new WeakReference<>(tank);
  }

  /**
   * Checks if this reference is still valid
   * @return  False if the stored tank is removed
   */
  public boolean isValid() {
    return this.tank.get() != null;
  }

  @Override
  public Fluid getFluid() {
    return Optional.ofNullable(tank.get())
                   .map(FluidTank::getFluid)
                   .map(FluidStack::getFluid)
                   .orElse(Fluids.EMPTY);
  }

  /* Melter methods */

  /**
   * Gets the contained fluid stack
   * @return  Contained fluid stack
   */
  public FluidStack getFluidStack() {
    return Optional.ofNullable(tank.get())
                   .map(FluidTank::getFluid)
                   .orElse(FluidStack.EMPTY);
  }

  /**
   * Gets the capacity of the contained tank
   * @return  Tank capacity
   */
  public int getCapacity() {
    return Optional.ofNullable(tank.get())
                   .map(tank -> (int) tank.getCapacity())
                   .orElse(0);
  }

  /**
   * Drains one copy of fuel from the given tank
   * @param fuel  Fuel to drain
   * @return  Ticks of fuel units
   */
  public int consumeFuel(MeltingFuel fuel) {
    FluidTank tank = this.tank.get();
    if (tank != null) {
      int amount = fuel.getAmount(this);
      if (amount > 0) {
        // TODO: assert drained valid?
        long drained;
        try (Transaction tx = TransferUtil.getTransaction()) {
          drained = tank.extract(tank.getFluid().getType(), amount, tx);
          tx.commit();
        }
        int duration = fuel.getDuration();
        if (drained < amount) {
          return (int) (duration * drained / amount);
        } else {
          return duration;
        }
      }
    }
    return 0;
  }
}
