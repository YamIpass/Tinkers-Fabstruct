package slimeknights.tconstruct.gadgets.block;

import io.github.fabricators_of_create.porting_lib.block.MinecartPassHandlerBlock;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.state.BlockState;

public class DropperRailBlock extends RailBlock implements MinecartPassHandlerBlock {

  public DropperRailBlock(Properties properties) {
    super(properties);
  }

  @Override
  public void onMinecartPass(BlockState state, Level world, BlockPos pos, AbstractMinecart cart) {
    // TODO(fabric): Forge's item handler capability on entities is replaced by the vanilla hopper minecart container
    if (!(cart instanceof Hopper) || !(cart instanceof Container cartInventory)) {
      return;
    }
    BlockEntity tileEntity = world.getBlockEntity(pos.below());
    if (tileEntity == null) {
      return;
    }
    Storage<ItemVariant> itemHandlerTE = TransferUtil.getItemStorage(world, pos.below(), tileEntity, Direction.UP);
    if (itemHandlerTE == null) {
      return;
    }

    // simulate: try inserting a single item into the block below, if it fits remove exactly that item from the cart
    for (int i = 0; i < cartInventory.getContainerSize(); i++) {
      ItemStack itemStack = cartInventory.getItem(i);
      if (itemStack.isEmpty()) {
        continue;
      }
      if (TransferUtil.insertItem(itemHandlerTE, itemStack.copyWithCount(1)) == 1) {
        cartInventory.removeItem(i, 1);
        break;
      }
    }
  }

}
