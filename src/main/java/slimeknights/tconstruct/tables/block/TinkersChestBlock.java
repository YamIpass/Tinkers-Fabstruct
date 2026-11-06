package slimeknights.tconstruct.tables.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;

public class TinkersChestBlock extends ChestBlock {
  public TinkersChestBlock(Properties builder, BlockEntitySupplier<? extends BlockEntity> be, boolean dropsItems) {
    super(builder, be, dropsItems);
  }

  @Override
  public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
    // TODO(fabric): Forge's getCloneItemStack(BlockState, HitResult, BlockGetter, BlockPos, Player) is vanilla's 3-arg form
    ItemStack stack = new ItemStack(this);
    if (world.getBlockEntity(pos) instanceof TinkersChestBlockEntity te && te.hasColor()) {
      ((DyeableLeatherItem) stack.getItem()).setColor(stack, te.getColor());
    }
    return stack;
  }
}
