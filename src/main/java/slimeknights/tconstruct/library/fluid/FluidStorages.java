package slimeknights.tconstruct.library.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Access helper for fluid storages.
 * <p>
 * The port keeps every internal tank in millibuckets, while the {@code FluidStorage.SIDED} lookup answers in droplets
 * (Fabric's fluid unit) because that is what foreign mods and Jade expect. Anything in this mod that reasons about its
 * own recipes (melting, casting, fuel, alloys) must therefore ask the block entity directly through {@link #raw} so it
 * keeps seeing millibuckets, and never through the droplet view.
 */
@SuppressWarnings("UnstableApiUsage")
public final class FluidStorages {
  private FluidStorages() {}

  /** Converts a droplet amount into the millibucket amount this port stores and displays */
  public static long toMillibuckets(long droplets) {
    return droplets <= 0 ? 0 : Math.max(1, droplets / 81);
  }

  /** Builds a millibucket fluid stack from a droplet based view, for callers whose API speaks millibuckets */
  public static io.github.fabricators_of_create.porting_lib.fluids.FluidStack stack(
    net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant> view) {
    return new io.github.fabricators_of_create.porting_lib.fluids.FluidStack(
      view.getResource().getFluid(), toMillibuckets(view.getAmount()));
  }

  /**
   * Bucket and container interaction for our own blocks.
   * <p>
   * A bucket holds 81000 droplets, so the container has to see the droplet view; handing it the raw millibucket tank
   * stores 81 times the amount the player actually poured. {@code interactWithContainer} handles filled buckets,
   * empty buckets and fluid items in one place.
   */
  public static boolean interactWithTankRaw(Level world, BlockPos pos, Player player, InteractionHand hand, Direction side) {
    Storage<FluidVariant> storage = FluidStorage.SIDED.find(world, pos, side);
    if (storage == null) {
      return false;
    }
    return slimeknights.mantle.fluid.FluidTransferHelper
      .interactWithContainer(world, pos, storage, player, hand)
      .hasContainer();
  }

  /** first non blank amount of a storage, in whatever unit that storage speaks */
  public static long firstAmount(Storage<FluidVariant> storage) {
    if (storage == null) {
      return -1;
    }
    for (net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant> view : storage) {
      if (!view.isResourceBlank()) {
        return view.getAmount();
      }
    }
    return 0;
  }

  /**
   * Gets a block entity's own storage, in the unit the port stores it in (millibuckets).
   * <p>
   * Foreign block entities keep answering through the regular lookup, which is in droplets; only our own block
   * entities are unwrapped.
   */
  public static Storage<FluidVariant> raw(Level level, BlockPos pos, Direction side) {
    if (level == null) {
      return null;
    }
    if (level.getBlockEntity(pos) instanceof MillibucketStorageBlockEntity stored) {
      return stored.getMillibucketStorage(side);
    }
    if (level.getBlockEntity(pos) instanceof SidedStorageBlockEntity sided) {
      return sided.getFluidStorage(side);
    }
    return FluidStorage.SIDED.find(level, pos, side);
  }
}
