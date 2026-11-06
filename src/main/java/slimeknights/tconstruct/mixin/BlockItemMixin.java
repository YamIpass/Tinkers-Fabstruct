package slimeknights.tconstruct.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Links a block to the block item that wraps it, reproducing the Forge patch on {@link BlockItem}.
 * <p>Vanilla only fills that link for its own items in {@code Items}, so without this every modded
 * block returns {@code Items.AIR} from {@link Block#asItem()} and {@code new ItemStack(block)} is
 * empty.</p>
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {
  @Inject(method = "<init>", at = @At("RETURN"))
  private void tconstruct$linkBlock(Block block, Item.Properties properties, CallbackInfo callback) {
    block.item = (BlockItem) (Object) this;
  }
}
