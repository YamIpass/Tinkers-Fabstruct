package slimeknights.tconstruct.world.item;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

/** Head item for enderman head, which counts as a pumpkin on the head */
public class EndermanHeadItem extends StandingAndWallBlockItem {
  public EndermanHeadItem(Block pBlock, Block pWallBlock, Properties pProperties, Direction pAttachmentDirection) {
    super(pBlock, pWallBlock, pProperties, pAttachmentDirection);
  }

    // TODO(fabric): Forge's Item#isEnderMask has no Fabric equivalent
    public boolean isEnderMask(ItemStack stack, Player player, EnderMan endermanEntity) {
    return true;
  }
}
