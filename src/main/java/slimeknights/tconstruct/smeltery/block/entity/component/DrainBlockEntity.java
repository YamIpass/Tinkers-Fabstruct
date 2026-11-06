package slimeknights.tconstruct.smeltery.block.entity.component;

import io.github.fabricators_of_create.porting_lib.block.CustomDataPacketHandlingBlockEntity;
import io.github.fabricators_of_create.porting_lib.block.CustomUpdateTagHandlingBlockEntity;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import javax.annotation.Nonnull;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryInputOutputBlockEntity.SmelteryFluidIO;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

/**
 * Fluid IO extension to display controller fluid
 */
public class DrainBlockEntity extends SmelteryFluidIO implements IDisplayFluidListener, CustomUpdateTagHandlingBlockEntity, CustomDataPacketHandlingBlockEntity {
  @Getter
  private FluidStack displayFluid = FluidStack.EMPTY;

  public DrainBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.drain.get(), pos, state);
  }

  protected DrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  // TODO(fabric): Forge's getModelData()/requestModelDataUpdate() have no Fabric equivalent; data is exposed
  //  through Fabric's RenderDataBlockEntity#getRenderData() and rebuilt on access
  @Nonnull
  @Override
  public Object getRenderData() {
    return RetexturedHelper.getModelDataBuilder(getTexture()).with(ModelProperties.FLUID_STACK, displayFluid).build();
  }

  @Override
  public void notifyDisplayFluidUpdated(FluidStack fluid) {
    if (!fluid.isFluidEqual(displayFluid)) {
      // no need to copy as the fluid was copied by the caller
      displayFluid = fluid;
      // TODO(fabric): ModelData is immutable in Mantle 1.11, the display fluid is read from getRenderData()
      assert level != null;
      BlockState state = getBlockState();
      level.sendBlockUpdated(worldPosition, state, state, 48);
    }
  }


  /* Updating */

  // override instead of writeSynced to avoid writing master to the main tag twice
  @Override
  public CompoundTag getUpdateTag() {
    CompoundTag nbt = super.getUpdateTag();
    writeMaster(nbt);
    return nbt;
  }

  // TODO(fabric): Forge synced block entities automatically; Mantle's custom update tag handling does it manually.
  //  The master block entity notifies display fluid listeners directly, so no listener re-attachment is needed.
  @Override
  public void handleUpdateTag(CompoundTag tag) {
    CustomUpdateTagHandlingBlockEntity.super.handleUpdateTag(tag);
  }

  // TODO(fabric): Forge synced block entities automatically; Mantle's custom data packet handling needs this hook
  @Override
  public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
    CompoundTag tag = pkt.getTag();
    if (tag != null) {
      load(tag);
    }
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }
}
