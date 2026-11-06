package slimeknights.tconstruct.smeltery.block.entity.tank;

import java.util.Collections;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.inventory.SingleItemHandler;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.IFluidTankUpdater;

/**
 * Fluid handler that proxies to an item stack tank.
 * <p>
 * TODO(fabric): Forge's {@code IFluidHandlerItem} has no Fabric equivalent; the nested item storage is fetched
 *  from {@link FluidStorage#ITEM} using a {@link ContainerItemContext} for this handler's slot, so tank items
 *  update the internal stack through a transaction instead of returning a container stack.
 */
public class ProxyItemTank<T extends MantleBlockEntity & IFluidTankUpdater> extends SingleItemHandler<T> {
  /** Context for the item in this handler's slot */
  private final ContainerItemContext context;
  @Nullable
  private Storage<FluidVariant> itemTank;
  /** Fluid view of the item in this handler's slot */
  private final FluidTankStorage fluidStorage = new FluidTankStorage();

  public ProxyItemTank(T parent) {
    super(parent, 1);
    this.context = ContainerItemContext.ofSingleSlot(getSlot(0));
  }

  @Override
  protected boolean isItemValid(ItemVariant variant) {
    // can only store items that are fluid handlers, though allow blacklist in case something is really broken
    // blacklist is mostly used for items that don't support incremental filling, as this block really isn't good at working with them
    // we check the container item so we don't have to put every bucket in the tag. Not bothering with complex container items; odds are item stack sensitive just returns the same item
    ItemStack stack = variant.toStack();
    ItemStack remainder = stack.getRecipeRemainder();
    return !stack.is(TinkerTags.Items.PROXY_TANK_BLACKLIST)
      && (remainder.isEmpty() || !RegistryHelper.contains(TinkerTags.Items.PROXY_TANK_BLACKLIST, remainder.getItem()))
      && (FluidStorage.ITEM.find(stack, context) != null);
  }

  /** Used by the fluid handler logic to sync changes as we directly mutate the internal stack */
  private void setStack(ItemStack newStack, boolean syncSame) {
    // if swapping to an empty stack, switch to the empty stack instance
    // prevents accidently having a 0 stack size capability
    if (newStack.isEmpty()) {
      newStack = ItemStack.EMPTY;
    }
    // update stack
    ItemStack oldStack = getStack();
    super.setStack(newStack);

    // server side may need to sync
    Level world = parent.getLevel();
    boolean needsUpdate = world != null && !world.isClientSide;
    if (oldStack != newStack) {
      // if the stack instance changed, discard cached cap and sync
      itemTank = null;
      if (needsUpdate) {
        // both stacks being empty means our stack shrunk by 1 and is being replaced with ItemStack.EMPTY
        needsUpdate = (oldStack.isEmpty() && newStack.isEmpty()) || !ItemStack.isSameItemSameTags(oldStack, newStack);
      }
    } else if (needsUpdate) {
      needsUpdate = syncSame;
    }
    // sync changes
    if (needsUpdate) {
      parent.onTankContentsChanged();
      BlockPos pos = parent.getBlockPos();
      TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(newStack, 0, pos), world, pos);
    }
  }

  @Override
  public void setStack(ItemStack newStack) {
    setStack(newStack, false);
  }

  /** Gets the fluid handler for the item, or null if the item has no fluid handler */
  @Nullable
  private Storage<FluidVariant> getItemTank() {
    if (itemTank == null) {
      ItemStack stack = getStack();
      if (!stack.isEmpty()) {
        itemTank = FluidStorage.ITEM.find(stack, context);
      }
    }
    return itemTank;
  }

  /** Gets the first storage view of the nested tank, or null if absent */
  @Nullable
  private StorageView<FluidVariant> findView() {
    Storage<FluidVariant> tank = getItemTank();
    if (tank != null) {
      Iterator<StorageView<FluidVariant>> iterator = tank.iterator();
      if (iterator.hasNext()) {
        return iterator.next();
      }
    }
    return null;
  }

  /** Gets the fluid storage for the item in this handler's slot, for use by the fluid transfer API */
  public Storage<FluidVariant> getFluidStorage() {
    return fluidStorage;
  }

  /** Gets the amount of fluid in the nested tank */
  public long getAmount() {
    return fluidStorage.getAmount();
  }

  /** Gets the capacity of the nested tank */
  public long getCapacity() {
    return fluidStorage.getCapacity();
  }

  /**
   * Adapter exposing the tank of the item in this handler's slot as a single slot fluid storage.
   * <p>
   * TODO(fabric): Forge's {@code IFluidHandlerItem} has no Fabric equivalent, so the nested tank cannot be inherited
   *  directly: {@link SingleItemHandler} already implements {@code SlottedStorage<ItemVariant>} and the fluid
   *  {@code Storage<FluidVariant>} view would clash on {@code iterator()}.
   */
  public class FluidTankStorage implements SingleSlotStorage<FluidVariant> {
    /* Delegation to the nested tank. Entities only support a single tank, matching Forge's IFluidHandlerItem */

    @Override
    public FluidVariant getResource() {
      StorageView<FluidVariant> view = findView();
      return view == null ? FluidVariant.blank() : view.getResource();
    }

    @Override
    public boolean isResourceBlank() {
      StorageView<FluidVariant> view = findView();
      return view == null || view.isResourceBlank();
    }

    @Override
    public long getAmount() {
      StorageView<FluidVariant> view = findView();
      return view == null ? 0 : view.getAmount();
    }

    @Override
    public long getCapacity() {
      StorageView<FluidVariant> view = findView();
      return view == null ? 0 : view.getCapacity();
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
      Storage<FluidVariant> tank = getItemTank();
      if (tank == null) {
        return 0;
      }
      long filled = tank.insert(resource, maxAmount, transaction);
      // if something happened, force a sync of the item stack
      // hopefully it's the same instance, but we still need a client sync likely
      if (filled > 0) {
        transaction.addCloseCallback((ctx, result) -> {
          if (result.wasCommitted()) {
            setStack(getStack(), true);
          }
        });
      }
      return filled;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
      Storage<FluidVariant> tank = getItemTank();
      if (tank == null) {
        return 0;
      }
      long drained = tank.extract(resource, maxAmount, transaction);
      if (drained > 0) {
        transaction.addCloseCallback((ctx, result) -> {
          if (result.wasCommitted()) {
            setStack(getStack(), true);
          }
        });
      }
      return drained;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
      Storage<FluidVariant> tank = getItemTank();
      return tank == null ? Collections.emptyIterator() : tank.iterator();
    }
  }
}
