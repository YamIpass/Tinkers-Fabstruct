package slimeknights.tconstruct.library.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Direction;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.smeltery.block.entity.tank.MillibucketStorage;

/**
 * A block entity whose tanks count millibuckets.
 * <p>
 * The port keeps every internal amount in millibuckets (recipes, fuel rates, casting costs), but Fabric's fluid unit is
 * the droplet and the sided lookup is what foreign mods and Jade read. Block entities therefore expose their own tank
 * through {@link #getMillibucketStorage(Direction)} for this mod's logic and let {@link #getFluidStorage(Direction)}
 * answer with the converted view, so a tooltip never renders fractions such as {@code 133 1/8 mB}.
 */
@SuppressWarnings("UnstableApiUsage")
public interface MillibucketStorageBlockEntity extends SidedStorageBlockEntity {
  /** the block entity's own storage, in millibuckets */
  @Nullable
  Storage<FluidVariant> getMillibucketStorage(@Nullable Direction side);

  @Override
  @Nullable
  default Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
    return MillibucketStorage.of(getMillibucketStorage(side));
  }
}
