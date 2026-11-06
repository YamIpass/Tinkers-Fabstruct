package slimeknights.tconstruct.library.fluid;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.minecraft.world.level.Level;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;

public class FluidTankBase<T extends MantleBlockEntity> extends FluidTank {

  protected T parent;

  public FluidTankBase(long capacity, T parent) {
    super(capacity);
    this.parent = parent;
  }

  // TODO(fabric): 3.12.1 overrode fill() to fix a bug in Forge's FluidTank where the amount returned by fill()
  //  could disagree with the fluid after onContentsChanged() ran. Porting Lib's FluidTank.insert() returns the
  //  amount it inserted independently of onContentsChanged(), so that bug does not apply here.
  @Override
  public void onContentsChanged() {
    if (parent instanceof IFluidTankUpdater) {
      ((IFluidTankUpdater) parent).onTankContentsChanged();
    }

    parent.setChanged();
    Level level = parent.getLevel();
    if(level != null && !level.isClientSide) {
      TinkerNetwork.getInstance().sendToClientsAround(new FluidUpdatePacket(parent.getBlockPos(), this.getFluid()), level, parent.getBlockPos());
    }
  }
}
