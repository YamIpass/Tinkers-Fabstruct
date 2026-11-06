package slimeknights.tconstruct.smeltery.item;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.CompoundTag;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;

/**
 * Fluid tank representing a stack of multiple fluid tanks. All operations must affect every tack in the stack at the same time, so must in increments of the scale.
 * Internally works the same as a fluid tank with {@code capacity * scale}, except operations are truncated to the nearest scale (e.g. if scale is 4, we must fill in 4mb increments).
 * <p>
 * TODO(fabric): Forge's {@code FluidTank} was int based with {@code fill}/{@code drain}; Porting Lib's version is long
 *  based and implements the Fabric transfer API, so the truncation is applied to the transaction based
 *  {@code insert}/{@code extract} instead.
 */
public class ScaledFluidTank extends FluidTank {
  private final int scale;
  private ScaledFluidTank(int capacity, int scale) {
    super((long) capacity * scale);
    this.scale = scale;
  }

  /** Creates a new instance */
  public static FluidTank create(int capacity, int scale) {
    if (scale == 1) {
      return new FluidTank(capacity);
    }
    return new ScaledFluidTank(capacity, scale);
  }

  /* Helpers */

  /** enforces the amount matches the scale */
  private long enforceScale(long amount) {
    // no working with fluids of partial amounts
    long remainder = amount % scale;
    if (remainder != 0) {
      amount -= remainder;
    }
    return amount;
  }

  /** enforces the fluid matches the scale */
  private FluidStack enforceScale(FluidStack stack, boolean copy) {
    // no working with fluids of partial amounts
    long remainder = stack.getAmount() % scale;
    if (remainder != 0) {
      if (copy) {
        stack = stack.copy();
      }
      stack.shrink(remainder);
    }
    return stack;
  }


  /* Fluid tank methods */

  @Override
  public FluidTank setCapacity(long capacity) {
    return super.setCapacity(enforceScale(capacity));
  }

  @Override
  public void setFluid(FluidStack stack) {
    super.setFluid(enforceScale(stack, false));
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return super.insert(resource, enforceScale(maxAmount), transaction);
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return super.extract(resource, enforceScale(maxAmount), transaction);
  }


  /* NBT */

  @Override
  public FluidTank readFromNBT(CompoundTag nbt) {
    // stored amount is for a single tank, scale it up to the full stack
    FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt);
    if (!fluid.isEmpty()) {
      fluid.setAmount(fluid.getAmount() * scale);
      setFluid(fluid);
    } else {
      setFluid(FluidStack.EMPTY);
    }
    return this;
  }

  @Override
  public CompoundTag writeToNBT(CompoundTag nbt) {
    // store the amount of a single tank, so different stack sizes can share data
    FluidStack fluid = getFluid();
    if (fluid.isEmpty()) {
      return nbt;
    }
    fluid = fluid.copy();
    fluid.setAmount(fluid.getAmount() / scale);
    return fluid.writeToNBT(nbt);
  }
}
