package slimeknights.tconstruct.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

/** This class exists simply to allow us to have a block entity renderer for obsidian gauges. Though it is useful as a cache for the capability to render. */
public class GaugeBlockEntity extends BlockEntity {
  // TODO(fabric): Forge cached a LazyOptional capability; Fabric looks up the storage each time it is invalidated
  @Nullable
  private Storage<FluidVariant> neighbor;
  public GaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public GaugeBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.gauge.get(), pos, state);
  }

  /** Gets the neighbor fluid handler. Used mainly for rendering client side */
  public Storage<FluidVariant> getTank() {
    if (level == null) {
      return Storage.empty();
    }
    // if we have not fetched the neighbor, fetch it
    if (neighbor == null) {
      Direction side = getBlockState().getValue(BlockStateProperties.FACING);
      neighbor = FluidStorage.SIDED.find(level, getBlockPos().relative(side.getOpposite()), side);
    }
    // return tank or empty tank
    return neighbor == null ? Storage.empty() : neighbor;
  }
}
