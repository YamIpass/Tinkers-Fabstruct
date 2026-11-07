package slimeknights.tconstruct.smeltery.block.entity.module;

import slimeknights.tconstruct.library.fluid.FluidStorages;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.StorageProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.block.entity.MantleBlockEntity;

/** Fuel module that supports multiple tanks, selecting just one for the fuel result */
public class MultitankFuelModule extends FuelModule implements Storage<FluidVariant> {
  /** Block position that will never be valid in world, used for sync */
  private static final BlockPos NULL_POS = new BlockPos(0, Short.MIN_VALUE, 0);

  /** Map of all tank handlers at each relevant position. Used for fast switching between handlers, notably in the UI */
  private Map<BlockPos,StorageProvider<FluidVariant>> tankHandlers;

  public MultitankFuelModule(MantleBlockEntity parent, Supplier<List<BlockPos>> tankSupplier) {
    super(parent, tankSupplier);
  }

  /** Resets just the last fluid listener */
  private void clearLastListener() {
    this.lastPos = NULL_POS;
    this.fluidHandler = null;
  }

  /** Called on structure rebuild to clear the gui handler list */
  public void clearFluidListeners() {
    tankHandlers = null;
  }

  /** Called on servant load to ensure the handler is present in the cache */
  public void ensureTankPresent(BlockEntity be) {
    BlockPos pos = be.getBlockPos();
    if (tankHandlers != null && !tankHandlers.containsKey(pos)) {
      // TODO(fabric): Forge capability listeners have no Fabric equivalent, the block api cache invalidates itself
      Storage<FluidVariant> handler = FluidStorages.raw(be.getLevel(), pos, null);
      if (handler != null) {
        tankHandlers.put(pos, StorageProvider.createForFluids(be.getLevel(), pos));
      }
    }
  }

  /** Gets the map from position to fluid handler */
  private Map<BlockPos,StorageProvider<FluidVariant>> getTankHandlers() {
    if (tankHandlers == null) {
      tankHandlers = new LinkedHashMap<>();
      Level world = getLevel();
      for (BlockPos pos : tankSupplier.get()) {
        if (FluidStorages.raw(world, pos, null) != null) {
          tankHandlers.put(pos, StorageProvider.createForFluids(world, pos));
        }
      }
    }
    return tankHandlers;
  }

  /** Gets the storage at the given position, null if none */
  private Storage<FluidVariant> getStorage(BlockPos pos) {
    StorageProvider<FluidVariant> provider = getTankHandlers().get(pos);
    return provider == null ? null : provider.get(null);
  }


  /* Fuel finding */

  /**
   * Tries to consume fuel from the given position
   * @param pos  Position
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  private int tryFuelPosition(BlockPos pos, boolean consume) {
    Storage<FluidVariant> tank = getStorage(pos);
    if (tank != null) {
      // if we find a valid cap, try to consume fuel from it
      int temperature = tryLiquidFuel(tank, consume);
      if (temperature > 0) {
        clearLastListener();
        fluidHandler = getTankHandlers().get(pos);
        lastPos = pos;
        return temperature;
      }
    }
    return 0;
  }

  /**
   * Attempts to consume fuel from one of the tanks
   * @return  temperature of the found fluid, 0 if none
   */
  @Override
  public int findFuel(boolean consume) {
    // only fetch a handler if we haven't done so
    if (fluidHandler != null) {
      // if we have a handler, try to use that if possible
      Storage<FluidVariant> storage = fluidHandler.get(null);
      if (storage != null) {
        int temperature = tryLiquidFuel(storage, consume);
        if (temperature > 0) {
          return temperature;
        }
      }
    } else if (lastPos != NULL_POS) {
      // if no handler, try to find one at the last position
      int posTemp = tryFuelPosition(lastPos, consume);
      if (posTemp > 0) {
        return posTemp;
      }
    }

    // find a new handler among our tanks
    for (BlockPos pos : tankSupplier.get()) {
      // already checked the last position above, no reason to try again
      if (!pos.equals(lastPos)) {
        int posTemp = tryFuelPosition(pos, consume);
        if (posTemp > 0) {
          return posTemp;
        }
      }
    }

    // no handler found, tell client of the lack of fuel
    if (consume) {
      temperature = 0;
      rate = 0;
    }
    return 0;
  }


  /* NBT */
  private static final String TAG_LAST_FUEL = "last_fuel";

  @Override
  public void readFromTag(CompoundTag nbt) {
    super.readFromTag(nbt);
    if (nbt.contains(TAG_LAST_FUEL, Tag.TAG_COMPOUND)) {
      lastPos = NbtUtils.readBlockPos(nbt.getCompound(TAG_LAST_FUEL)).offset(parent.getBlockPos());
    }
  }

  @Override
  public CompoundTag writeToTag(CompoundTag nbt) {
    nbt = super.writeToTag(nbt);
    if (lastPos != NULL_POS) {
      nbt.put(TAG_LAST_FUEL, NbtUtils.writeBlockPos(lastPos.subtract(parent.getBlockPos())));
    }
    return nbt;
  }


  /* UI syncing */
  private static final int LAST_X = 4;
  private static final int LAST_Y = 5;
  private static final int LAST_Z = 6;

  @Override
  public int getCount() {
    return 7;
  }

  @Override
  public int get(int index) {
    return switch (index) {
      case LAST_X -> lastPos.getX();
      case LAST_Y -> lastPos.getY();
      case LAST_Z -> lastPos.getZ();
      default -> super.get(index);
    };
  }

  @Override
  public void set(int index, int value) {
    if (LAST_X <= index && index <= LAST_Z) {
      switch (index) {
        case LAST_X -> lastPos = new BlockPos(value, lastPos.getY(), lastPos.getZ());
        case LAST_Y -> lastPos = new BlockPos(lastPos.getX(), value, lastPos.getZ());
        case LAST_Z -> lastPos = new BlockPos(lastPos.getX(), lastPos.getY(), value);
      }
      clearLastListener();
    } else {
      super.set(index, value);
    }
  }

  @Override
  public FuelInfo getFuelInfo() {
    // fetch the primary fuel handler, the tank we last pulled fuel from
    // Y of big negative is how the UI syncs null, meaning we have not yet consumed fuel
    if (fluidHandler == null && lastPos.getY() != NULL_POS.getY()) {
      StorageProvider<FluidVariant> fluidCap = getTankHandlers().get(lastPos);
      if (fluidCap != null) {
        fluidHandler = fluidCap;
      }
    }

    // two cases leave us with no tank to show despite the structure holding fuel: we never consumed fuel so we have no handler,
    // and the tank we did consume from was drained dry while we are still burning the fuel we took out of it
    // in both cases promote the first tank containing fluid, caching the handler so the search does not repeat next frame
    // note we search for fluid rather than for fuel, skipping the recipe lookup as there is little use in non-fuels in the smeltery walls
    Storage<FluidVariant> storage = fluidHandler == null ? null : fluidHandler.get(null);
    boolean hasFluid = false;
    if (storage != null) {
      StorageView<FluidVariant> view = firstView(storage);
      hasFluid = view != null && !new FluidStack(view).isEmpty();
    }
    if (!hasFluid) {
      for (StorageProvider<FluidVariant> tankCap : getTankHandlers().values()) {
        Storage<FluidVariant> tankStorage = tankCap.get(null);
        if (tankStorage != null) {
          StorageView<FluidVariant> view = firstView(tankStorage);
          if (view != null && !new FluidStack(view).isEmpty()) {
            clearLastListener();
            fluidHandler = tankCap;
            break;
          }
        }
      }
      // if no tank contains fluid we cache nothing, so the search runs again next frame until fuel returns
      // caching the lack of a tank saves that loop, but leaves the gauge empty after refueling a structure that ran dry
    }

    // determine what fluid we have and hpw many other fluids we have
    FuelInfo info = super.getFuelInfo();
    // add extra fluid display
    if (!info.isEmpty()) {
      // add display info from each handler
      FluidStack currentFuel = info.getFluid();
      for (StorageProvider<FluidVariant> tankCap : getTankHandlers().values()) {
        // skip the main tank, the info above is already its contents
        if (tankCap != fluidHandler) {
          Storage<FluidVariant> tankStorage = tankCap.get(null);
          if (tankStorage == null) {
            continue;
          }
          // sum if empty (more capacity) or the same fluid (more amount and capacity)
          StorageView<FluidVariant> view = firstView(tankStorage);
          if (view != null) {
            FluidStack fluid = new FluidStack(view);
            if (fluid.isEmpty()) {
              info.add(0, view.getCapacity());
            } else if (currentFuel.isFluidEqual(fluid)) {
              info.add(fluid.getAmount(), view.getCapacity());
            }
          }
        }
      }
    }

    return info;
  }

  /** Gets the first view of the given storage, may be null */
  private static StorageView<FluidVariant> firstView(Storage<FluidVariant> storage) {
    Iterator<StorageView<FluidVariant>> iterator = storage.iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }


  /* Fluid handler */

  /** Gets the most recently used fluid */
  public FluidStack getLastFluid() {
    if (fluidHandler != null) {
      Storage<FluidVariant> storage = fluidHandler.get(null);
      if (storage != null) {
        StorageView<FluidVariant> view = firstView(storage);
        if (view != null) {
          return FluidStorages.stack(view);
        }
      }
    }
    BlockPos pos;
    if (lastPos.getY() != NULL_POS.getY()) {
      pos = lastPos;
    } else {
      List<BlockPos> positions = tankSupplier.get();
      if (!positions.isEmpty()) {
        pos = positions.get(0);
      } else {
        return FluidStack.EMPTY;
      }
    }
    Storage<FluidVariant> storage = getStorage(pos);
    if (storage == null) {
      return FluidStack.EMPTY;
    }
    StorageView<FluidVariant> view = firstView(storage);
    return view == null ? FluidStack.EMPTY : FluidStorages.stack(view);
  }

  /** Gets all tank storages, in tank order */
  private List<Storage<FluidVariant>> getStorages() {
    List<Storage<FluidVariant>> storages = new ArrayList<>();
    for (BlockPos pos : tankSupplier.get()) {
      Storage<FluidVariant> storage = getStorage(pos);
      if (storage != null) {
        storages.add(storage);
      }
    }
    return storages;
  }

  @Override
  public Iterator<StorageView<FluidVariant>> iterator() {
    List<StorageView<FluidVariant>> views = new ArrayList<>();
    for (Storage<FluidVariant> storage : getStorages()) {
      for (StorageView<FluidVariant> view : storage) {
        views.add(view);
      }
    }
    return views.iterator();
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    long totalFilled = 0;
    // try each handler, updating the amount we filled as we go
    // note the map internally is a linked hash map so order is consistent
    for (Storage<FluidVariant> storage : getStorages()) {
      long filled = storage.insert(resource, maxAmount - totalFilled, transaction);
      if (filled > 0) {
        // if we finished filling, we are done
        totalFilled += filled;
        if (totalFilled >= maxAmount) {
          break;
        }
      }
    }
    return totalFilled;
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    long totalDrained = 0;
    // try each handler, updating the amount we drained as we go
    // note the map internally is a linked hash map so order is consistent
    for (Storage<FluidVariant> storage : getStorages()) {
      long drained = storage.extract(resource, maxAmount - totalDrained, transaction);
      if (drained > 0) {
        totalDrained += drained;
        // if we drained everything desired, we are done
        if (totalDrained >= maxAmount) {
          break;
        }
      }
    }
    return totalDrained;
  }
}
