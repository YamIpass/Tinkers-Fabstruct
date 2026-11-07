package slimeknights.tconstruct.smeltery.block.entity.tank;

import java.util.Iterator;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * Views a tank that counts in millibuckets through Fabric's droplet unit, which is what every other mod (and Jade's
 * fluid tooltip) expects from a {@link Storage}. One millibucket is 81 droplets.
 *
 * <p>Tinkers' internals were written against Forge, where the unit is the millibucket; exposing the raw tank to the
 * Fabric lookup made Jade print the capacity in buckets with a millibucket label.</p>
 */
public record MillibucketStorage(Storage<FluidVariant> delegate) implements Storage<FluidVariant> {
  /** droplets per millibucket */
  public static final long DROPLETS_PER_MB = 81;

  /** wraps a millibucket storage in the droplet view, tolerating null and already wrapped storages */
  public static Storage<FluidVariant> of(Storage<FluidVariant> storage) {
    if (storage == null || storage instanceof MillibucketStorage) {
      return storage;
    }
    return new MillibucketStorage(storage);
  }

  /** converts a droplet count to millibuckets, never rounding a non empty amount down to nothing */
  private static long toMb(long droplets) {
    long mb = droplets / DROPLETS_PER_MB;
    return droplets > 0 && mb == 0 ? 1 : mb;
  }

  @Override
  public boolean supportsInsertion() {
    return delegate.supportsInsertion();
  }

  @Override
  public boolean supportsExtraction() {
    return delegate.supportsExtraction();
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return delegate.insert(resource, toMb(maxAmount), transaction) * DROPLETS_PER_MB;
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return delegate.extract(resource, toMb(maxAmount), transaction) * DROPLETS_PER_MB;
  }

  @Override
  public Iterator<StorageView<FluidVariant>> iterator() {
    Iterator<StorageView<FluidVariant>> views = delegate.iterator();
    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return views.hasNext();
      }

      @Override
      public StorageView<FluidVariant> next() {
        return new DropletView(views.next());
      }
    };
  }

  /** one slot, reported in droplets */
  private record DropletView(StorageView<FluidVariant> delegate) implements StorageView<FluidVariant> {
    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
      return delegate.extract(resource, toMb(maxAmount), transaction) * DROPLETS_PER_MB;
    }

    @Override
    public boolean isResourceBlank() {
      return delegate.isResourceBlank();
    }

    @Override
    public FluidVariant getResource() {
      return delegate.getResource();
    }

    @Override
    public long getAmount() {
      return delegate.getAmount() * DROPLETS_PER_MB;
    }

    @Override
    public long getCapacity() {
      return delegate.getCapacity() * DROPLETS_PER_MB;
    }
  }
}
