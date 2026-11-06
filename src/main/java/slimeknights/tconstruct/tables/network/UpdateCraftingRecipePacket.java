package slimeknights.tconstruct.tables.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.network.packet.ISimplePacket.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;

/**
 * Packet to send the current crafting recipe to a player who opens the crafting station.
 * TODO 1.21: make record.
 */
public class UpdateCraftingRecipePacket implements IThreadsafePacket {
  private final BlockPos pos;
  private final ResourceLocation recipe;
  public UpdateCraftingRecipePacket(BlockPos pos, CraftingRecipe recipe) {
    this.pos = pos;
    this.recipe = recipe.getId();
  }

  public UpdateCraftingRecipePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.recipe = buffer.readResourceLocation();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeResourceLocation(recipe);
  }

  @Override
  public void handleThreadsafe(Context context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(UpdateCraftingRecipePacket packet) {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
        // TODO(fabric): Forge's BlockEntityPacket#getBlockEntity is replaced with Mantle's BlockEntityHelper
        CraftingRecipe recipe = RecipeHelper.getRecipe(level.getRecipeManager(), packet.recipe, CraftingRecipe.class).orElse(null);
        if (recipe != null) {
          BlockEntityHelper.get(CraftingStationBlockEntity.class, level, packet.pos).ifPresent(be -> be.updateRecipe(recipe));
        } else {
          TConstruct.LOG.error("Failed to update Crafting Station Recipe at {}: unknown recipe {}", packet.pos, packet.recipe);
        }
      }
    }
  }
}
