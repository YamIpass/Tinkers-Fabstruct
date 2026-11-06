package slimeknights.tconstruct.library.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import javax.annotation.Nonnull;

/**
 * Simple implementation of a single slot fluid tank, usable as a Fabric {@link SingleSlotStorage}.
 *
 * Similar to {@link net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage} except with more control over the fluid storage.
 */
public interface SimpleFluidTank extends SingleSlotStorage<FluidVariant> {
  /** Gets the fluid in the tank */
  @Nonnull
  FluidStack getFluid();

  /** Called to set the fluid after it has changed */
  void setFluid(FluidStack fluid);

  /** Gets the maximum amount of fluid this tank can hold */
  @Override
  long getCapacity();

  /**
   * Used by {@link #insert(FluidVariant, long, TransactionContext)} and {@link #extract(FluidVariant, long, TransactionContext)} to update the fluid result.
   * Allows updating the fluid without needing to call {@link #getFluid()} again, in case it has a cost.
   * @param updated  New fluid stack
   * @param change   Amount the fluid grew or shrunk by.
   */
  default void updateFluid(FluidStack updated, int change) {
    if (change != 0) {
      setFluid(updated);
    }
  }


  /* Redirect duplicate methods */

  /** Gets the amount of fluid in the tank */
  default int getFluidAmount() {
    return (int)getFluid().getAmount();
  }

  @Override
  default long getAmount() {
    return getFluid().getAmount();
  }

  @Override
  default FluidVariant getResource() {
    FluidStack fluid = getFluid();
    if (fluid.isEmpty()) {
      return FluidVariant.blank();
    }
    return FluidVariant.of(fluid.getFluid(), fluid.getTag());
  }

  @Override
  default boolean isResourceBlank() {
    return getFluid().isEmpty();
  }

  /** Checks if the given fluid may be inserted into this tank */
  default boolean isFluidValid(FluidStack stack) {
    return true;
  }


  /* Filling and draining */

  @Override
  default long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    if (resource.isBlank() || maxAmount <= 0 || !isFluidValid(new FluidStack(resource, maxAmount))) {
      return 0;
    }
    FluidStack fluid = getFluid();
    FluidStack updated;
    long inserted;
    if (fluid.isEmpty()) {
      // if we have nothing, fill as much as possible
      inserted = Math.min(getCapacity(), maxAmount);
      updated = new FluidStack(resource, inserted);
    } else {
      // if unable to fill, nothing more to do
      if (!fluid.isFluidEqual(resource)) {
        return 0;
      }
      inserted = Math.min(getCapacity() - fluid.getAmount(), maxAmount);
      updated = new FluidStack(fluid, fluid.getAmount() + inserted);
    }
    if (inserted <= 0) {
      return 0;
    }
    setFluidOnCommit(transaction, updated, (int)inserted);
    return inserted;
  }

  @Override
  default long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    if (resource.isBlank() || maxAmount <= 0) {
      return 0;
    }
    FluidStack fluid = getFluid();
    if (fluid.isEmpty() || !fluid.isFluidEqual(resource)) {
      return 0;
    }
    return drainFluid(fluid, maxAmount, transaction).getAmount();
  }

  /** Drains up to maxDrain fluid of the given type */
  @Nonnull
  default FluidStack drain(FluidVariant resource, long maxDrain, TransactionContext transaction) {
    if (resource.isBlank() || maxDrain <= 0) {
      return FluidStack.EMPTY;
    }
    FluidStack fluid = getFluid();
    if (fluid.isEmpty() || !fluid.isFluidEqual(resource)) {
      return FluidStack.EMPTY;
    }
    return drainFluid(fluid, maxDrain, transaction);
  }

  /** Drains up to maxDrain fluid out of the tank, ignoring the fluid type */
  @Nonnull
  default FluidStack drain(long maxDrain, TransactionContext transaction) {
    FluidStack fluid = getFluid();
    if (maxDrain <= 0 || fluid.isEmpty()) {
      return FluidStack.EMPTY;
    }
    return drainFluid(fluid, maxDrain, transaction);
  }

  /** Common logic between all drain methods, fluid is known to be non-empty */
  private FluidStack drainFluid(FluidStack fluid, long maxDrain, TransactionContext transaction) {
    long drained = Math.min(fluid.getAmount(), maxDrain);
    if (drained <= 0) {
      return FluidStack.EMPTY;
    }
    FluidStack result = new FluidStack(fluid, drained);
    if (drained >= fluid.getAmount()) {
      setFluidOnCommit(transaction, FluidStack.EMPTY, (int)-drained);
    } else {
      setFluidOnCommit(transaction, new FluidStack(fluid, fluid.getAmount() - drained), (int)-drained);
    }
    return result;
  }

  /**
   * Applies the given fluid change when the passed transaction commits. This keeps simulate calls (which abort their
   * transaction) from mutating the tank, while still supporting immediate updates when no transaction is passed.
   */
  private void setFluidOnCommit(TransactionContext transaction, FluidStack updated, int change) {
    if (transaction == null) {
      updateFluid(updated, change);
    } else {
      transaction.addCloseCallback((tx, result) -> {
        if (result.wasCommitted()) {
          updateFluid(updated, change);
        }
      });
    }
  }
}
