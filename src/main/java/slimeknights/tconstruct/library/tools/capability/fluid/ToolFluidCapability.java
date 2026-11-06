package slimeknights.tconstruct.library.tools.capability.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider.IToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Logic to make a tool a fluid handler
 */
@RequiredArgsConstructor
public class ToolFluidCapability extends FluidModifierHookIterator<ModifierEntry> implements SingleSlotStorage<FluidVariant> {
  /** Boolean key to set in volatile mod data to enable the fluid capability */
  public static final ResourceLocation TOTAL_TANKS = TConstruct.getResource("total_tanks");

  /** Modifier hook instance to make an inventory modifier */
  public static final ModuleHook<FluidModifierHook> HOOK = ModifierHooks.register(TConstruct.getResource("fluid"), FluidModifierHook.class, FluidModifierHookMerger::new, new FluidModifierHook() {
    @Override
    public int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      return 0;
    }

    @Override
    public boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      return false;
    }

    @Override
    public int fill(IToolStackView tool, ModifierEntry modifier, FluidStack resource, boolean simulate) {
      return 0;
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, FluidStack resource, boolean simulate) {
      return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, int maxDrain, boolean simulate) {
      return FluidStack.EMPTY;
    }
  });

  @Getter
  private final ItemStack container;
  private final Supplier<? extends IToolStackView> tool;

  /* Basic inventory */

  @Override
  public int getSlotCount() {
    return tool.get().getVolatileData().getInt(TOTAL_TANKS);
  }

  /** Gets the number of tanks on this tool, used in place of {@link #getSlotCount()} prior to the transfer API */
  public int getTanks() {
    return getSlotCount();
  }

  @Override
  protected Iterator<ModifierEntry> getIterator(IToolStackView tool) {
    return tool.getModifierList().iterator();
  }

  @Override
  protected FluidModifierHook getHook(ModifierEntry entry) {
    indexEntry = entry;
    return entry.getHook(HOOK);
  }

  @Override
  public SingleSlotStorage<FluidVariant> getSlot(int tank) {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, tank);
    if (hook != null) {
      return new TankView(tool, hook, indexEntry, tank - startIndex);
    }
    return SingleFluidStorage.withFixedCapacity(0, () -> {});
  }

  // TODO(fabric): these are the Forge IFluidHandler surface; on Fabric the capability is a
  // SingleSlotStorage<FluidVariant> using insert/extract, so they stay as plain helpers for now
  @Nonnull
  public FluidStack getFluidInTank(int tank) {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, tank);
    if (hook != null) {
      return hook.getFluidInTank(tool, indexEntry, tank - startIndex);
    }
    return FluidStack.EMPTY;
  }

  public int getTankCapacity(int tank) {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, tank);
    if (hook != null) {
      return hook.getTankCapacity(tool, indexEntry, tank - startIndex);
    }
    return 0;
  }

  public boolean isFluidValid(int tank, FluidStack stack) {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, tank);
    if (hook != null) {
      return hook.isFluidValid(tool, indexEntry, tank - startIndex, stack);
    }
    return false;
  }

  public int fill(FluidStack resource, boolean simulate) {
    return fill(tool.get(), resource, simulate);
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    if (resource.isBlank() || maxAmount <= 0) {
      return 0;
    }
    // TODO(fabric): the transfer API cannot modify the item when no container context is available; the fluid NBT is written directly like the Forge capability
    return fill(tool.get(), new FluidStack(resource, maxAmount), false);
  }

  /** Scales the result for the given stack size */
  private static FluidStack scaleResult(FluidStack stack, int size) {
    if (size > 1 && !stack.isEmpty()) {
      stack.setAmount(stack.getAmount() * size);
    }
    return stack;
  }

  @Nonnull
  public FluidStack drain(FluidStack resource, boolean simulate) {
    if (resource.isEmpty()) {
      return FluidStack.EMPTY;
    }
    int size = container.getCount();
    if (size > 1) {
      resource = new FluidStack(resource, resource.getAmount() / size);
    }
    return scaleResult(drain(tool.get(), resource, simulate), size);
  }

  @Nonnull
  public FluidStack drain(int maxDrain, boolean simulate) {
    if (maxDrain < 0) {
      return FluidStack.EMPTY;
    }
    int size = container.getCount();
    return scaleResult(drain(tool.get(), maxDrain / size, simulate), size);
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    if (resource.isBlank() || maxAmount <= 0) {
      return 0;
    }
    return drain(tool.get(), new FluidStack(resource, maxAmount), false).getAmount();
  }

  @Override
  public boolean isResourceBlank() {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, 0);
    if (hook != null) {
      return hook.getFluidInTank(tool, indexEntry, startIndex).getType().isBlank();
    }
    return true;
  }

  @Override
  public FluidVariant getResource() {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, 0);
    if (hook != null) {
      return hook.getFluidInTank(tool, indexEntry, startIndex).getType();
    }
    return FluidVariant.blank();
  }

  @Override
  public long getAmount() {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, 0);
    if (hook != null) {
      return hook.getFluidInTank(tool, indexEntry, startIndex).getAmount();
    }
    return 0;
  }

  @Override
  public long getCapacity() {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, 0);
    if (hook != null) {
      return hook.getTankCapacity(tool, indexEntry, startIndex);
    }
    return 0;
  }

  /** Adds the tanks from the fluid modifier to the tool */
  public static void addTanks(ModifierEntry modifier, ModDataNBT volatileData, FluidModifierHook hook) {
    volatileData.putInt(TOTAL_TANKS, hook.getTanks(volatileData, modifier) + volatileData.getInt(TOTAL_TANKS));
  }

  /**
   * Interface for modifiers with fluid capabilities to return.
   * @deprecated We are considering removing this interface in favor of a much simpler tool tank implementation.
   * For most use-cases {@link ToolTankHelper} is sufficient. If it does not cover your usecase, please leave a comment on <a href="https://github.com/SlimeKnights/TinkersConstruct/issues/5353">GitHub #5353</a>.
   */
  @SuppressWarnings("unused")
  @Deprecated
  public interface FluidModifierHook {
    /**
     * Determines how many fluid tanks are used by this modifier
     * @param volatileData  Tool data to check
     * @param modifier      Modifier to consider
     * @return  Number of tanks used
     */
    default int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      return 1;
    }

    /**
     * Gets the fluid in the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @return  Fluid in the given tank
     */
    default FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
      return FluidStack.EMPTY;
    }

    /**
     * Gets the max capacity for the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @return  Fluid in the given tank
     */
    default int getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      return 0;
    }

    /**
     * Checks if the fluid is valid for the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @param fluid  Fluid to insert
     * @return  True if the fluid is valid
     */
    default boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      return true;
    }

    /**
     * Fills fluid into tanks
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param resource  FluidStack representing the Fluid and maximum amount of fluid to be filled. If you want to store this stack, make a copy
     * @param simulate  If true, fill will only be simulated.
     * @return Amount of resource that was (or would have been, if simulated) filled.
     */
    int fill(IToolStackView tool, ModifierEntry modifier, FluidStack resource, boolean simulate);

    /**
     * Drains fluid out of tanks, distribution is left entirely to the fluid handler.
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param resource  FluidStack representing the Fluid and maximum amount of fluid to be drained.
     * @param simulate  If true, drain will only be simulated.
     * @return FluidStack representing the Fluid and amount that was (or would have been, if
     * simulated) drained.
     */
    FluidStack drain(IToolStackView tool, ModifierEntry modifier, FluidStack resource, boolean simulate);

    /**
     * Drains fluid out of internal tanks, distribution is left entirely to the fluid handler.
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param maxDrain  Maximum amount of fluid to drain.
     * @param simulate  If true, drain will only be simulated.
     * @return FluidStack representing the Fluid and amount that was (or would have been, if
     * simulated) drained.
     */
    FluidStack drain(IToolStackView tool, ModifierEntry modifier, int maxDrain, boolean simulate);
  }

  /** Single slot storage view of one tank on the tool */
  @RequiredArgsConstructor
  private final class TankView implements SingleSlotStorage<FluidVariant> {
    private final IToolStackView tool;
    private final FluidModifierHook hook;
    private final ModifierEntry modifier;
    private final int tank;

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
      if (resource.isBlank() || maxAmount <= 0) {
        return 0;
      }
      FluidStack toFill = new FluidStack(resource, maxAmount);
      if (!hook.isFluidValid(tool, modifier, tank, toFill)) {
        return 0;
      }
      int filled = hook.fill(tool, modifier, toFill, true);
      if (filled <= 0) {
        return 0;
      }
      return hook.fill(tool, modifier, new FluidStack(resource, filled), false);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
      if (resource.isBlank() || maxAmount <= 0) {
        return 0;
      }
      FluidStack simulated = hook.drain(tool, modifier, new FluidStack(resource, maxAmount), true);
      if (simulated.isEmpty()) {
        return 0;
      }
      return hook.drain(tool, modifier, new FluidStack(resource, simulated.getAmount()), false).getAmount();
    }

    @Override
    public boolean isResourceBlank() {
      return hook.getFluidInTank(tool, modifier, tank).getType().isBlank();
    }

    @Override
    public FluidVariant getResource() {
      return hook.getFluidInTank(tool, modifier, tank).getType();
    }

    @Override
    public long getAmount() {
      return hook.getFluidInTank(tool, modifier, tank).getAmount();
    }

    @Override
    public long getCapacity() {
      return hook.getTankCapacity(tool, modifier, tank);
    }
  }

  /** Logic to merge multiple fluid hooks */
  @RequiredArgsConstructor
  private static class FluidModifierHookMerger extends FluidModifierHookIterator<FluidModifierHook> implements FluidModifierHook {
    private final Collection<FluidModifierHook> modules;

    @Override
    protected Iterator<FluidModifierHook> getIterator(IToolStackView tool) {
      return modules.iterator();
    }

    @Override
    protected FluidModifierHook getHook(FluidModifierHook entry) {
      return entry;
    }

    /** Gets the given hook */
    @Nullable
    private FluidModifierHook findHook(IToolStackView tool, ModifierEntry modifier, int tank) {
      indexEntry = modifier;
      return this.findHook(tool, tank);
    }

    @Override
    public int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      int sum = 0;
      for (FluidModifierHook module : modules) {
        sum += module.getTanks(volatileData, modifier);
      }
      return sum;
    }

    @Override
    public FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.getFluidInTank(tool, modifier, tank - startIndex);
      }
      return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.getTankCapacity(tool, modifier, tank - startIndex);
      }
      return 0;
    }

    @Override
    public boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.isFluidValid(tool, modifier, tank - startIndex, fluid);
      }
      return false;
    }

    @Override
    public int fill(IToolStackView tool, ModifierEntry modifier, FluidStack resource, boolean simulate) {
      indexEntry = modifier;
      return fill(tool, resource, simulate);
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, FluidStack resource, boolean simulate) {
      indexEntry = modifier;
      return drain(tool, resource, simulate);
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, int maxDrain, boolean simulate) {
      indexEntry = modifier;
      return drain(tool, maxDrain, simulate);
    }
  }

  /** Provider instance for a fluid cap */
  public static class Provider implements IToolCapabilityProvider {
    public Provider(ItemStack stack, Supplier<? extends IToolStackView> toolStack) {
      // TODO(fabric): item capabilities are registered directly in the item constructor, this provider is only kept for API compatibility
    }
  }
}
