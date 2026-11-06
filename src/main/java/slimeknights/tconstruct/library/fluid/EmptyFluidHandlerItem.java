package slimeknights.tconstruct.library.fluid;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Iterator;

/** Empty fluid handler item instance, usable like an empty tank. */
@RequiredArgsConstructor
public class EmptyFluidHandlerItem implements Storage<FluidVariant> {
  public static final EmptyFluidHandlerItem INSTANCE = new EmptyFluidHandlerItem(ItemStack.EMPTY);

  /** Container reference */
  @Getter
  private final ItemStack container;

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public Iterator<StorageView<FluidVariant>> iterator() {
    return Collections.emptyIterator();
  }
}
