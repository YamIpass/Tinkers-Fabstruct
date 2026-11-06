package slimeknights.tconstruct.smeltery.block.entity.inventory;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.tconstruct.library.fluid.IMultitankListChange;

@AllArgsConstructor
public class DuctTankWrapper implements SlottedStorage<FluidVariant> { // Fabric has FilteringStorage but in order to not create merge conflicts we use our own class
  private final SlottedStorage<FluidVariant> parent;
  private final DuctItemHandler itemHandler;
  private int[] tankMapping;

  // TODO(fabric): Forge passed an IFluidHandler here; Fabric passes the slotted storage of the smeltery tank
  public DuctTankWrapper(SlottedStorage<FluidVariant> parent, DuctItemHandler itemHandler) {
    this.parent = parent;
    this.itemHandler = itemHandler;
    // clear cache when the fluid changes or the smeltery list changes
    Consumer<DuctTankWrapper> consumer = self -> self.tankMapping = null;
    itemHandler.addListener(this, consumer);
    if (parent instanceof IMultitankListChange notifier) {
      notifier.addTankListListener(this, consumer);
    }
  }

  /** Gets the mapping from index to matching tank */
  private int[] getTankMapping() {
    if (tankMapping == null) {
      FluidStack filter = itemHandler.getFluid();
      int count = parent.getSlotCount();
      if (filter.isEmpty()) {
        FluidStack last = new FluidStack(parent.getSlot(count - 1));
        if (last.isEmpty()) {
          tankMapping = new int[] { count - 1 };
        } else {
          tankMapping = new int[0];
        }
      } else {
        IntList list = new IntArrayList(count);
        for (int i = 0; i < count; i++) {
          FluidStack contained = new FluidStack(parent.getSlot(i));
          if (contained.isEmpty() || filter.isFluidEqual(contained)) {
            list.add(i);
          }
        }
        tankMapping = list.toIntArray();
      }
    }
    return tankMapping;
  }


  /* Properties */

  @Override
  public int getSlotCount() {
    return parent.getSlotCount();
  }

  @Override
  public SingleSlotStorage<FluidVariant> getSlot(int tank) {
    return parent.getSlot(tank);
  }

  @Override
  public Iterator<StorageView<FluidVariant>> iterator() {
    return Iterators.transform(parent.iterator(), FilteringStorageView::new);
  }


  /* Interactions */

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    if ((maxAmount <= 0 || resource.isBlank()) || !itemHandler.getFluid().isFluidEqual(resource)) {
      return 0;
    }
    return parent.insert(resource, maxAmount, transaction);
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    if ((maxAmount <= 0 || resource.isBlank()) || !itemHandler.getFluid().isFluidEqual(resource)) {
      return 0;
    }
    return parent.extract(resource, maxAmount, transaction);
  }

  /**
   * Fabric copy of {@link net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage.FilteringStorageView}
   */
  private class FilteringStorageView implements StorageView<FluidVariant> {
    private final StorageView<FluidVariant> backingView;

    private FilteringStorageView(StorageView<FluidVariant> backingView) {
      this.backingView = backingView;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
      if ((maxAmount <= 0 || resource.isBlank()) || !itemHandler.getFluid().isFluidEqual(resource)) {
        return 0;
      }
      return backingView.extract(resource, maxAmount, transaction);
    }

    @Override
    public boolean isResourceBlank() {
      return backingView.isResourceBlank();
    }

    @Override
    public FluidVariant getResource() {
      return backingView.getResource();
    }

    @Override
    public long getAmount() {
      return backingView.getAmount();
    }

    @Override
    public long getCapacity() {
      return backingView.getCapacity();
    }

    @Override
    public StorageView<FluidVariant> getUnderlyingView() {
      return backingView.getUnderlyingView();
    }
  }
}
