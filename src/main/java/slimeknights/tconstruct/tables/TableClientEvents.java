package slimeknights.tconstruct.tables;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.client.render.InventoryBlockEntityRenderer;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.CraftingStationScreen;
import slimeknights.tconstruct.tables.client.inventory.ModifierWorktableScreen;
import slimeknights.tconstruct.tables.client.inventory.PartBuilderScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerChestScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

@SuppressWarnings("unused")
public class TableClientEvents extends ClientEventBase {
  /**
   * Registers the client events
   * TODO(fabric): Forge ran these from the mod event bus; the Fabric port calls this from the client entrypoint
   *  (see {@link slimeknights.tconstruct.shared.TinkerClient#onConstruct()})
   */
  public static void init() {
    registerRenderers();
    setupClient();
    registerBlockColors();
    registerItemColors();
  }

  static void registerRenderers() {
    // TODO(fabric): Forge's EntityRenderersEvent.RegisterRenderers was replaced with vanilla's BlockEntityRenderers
    BlockEntityRendererProvider<TableBlockEntity> tableRenderer = InventoryBlockEntityRenderer::new;
    BlockEntityRenderers.register(TinkerTables.craftingStationTile.get(), tableRenderer);
    BlockEntityRenderers.register(TinkerTables.tinkerStationTile.get(), tableRenderer);
    BlockEntityRenderers.register(TinkerTables.modifierWorktableTile.get(), tableRenderer);
    BlockEntityRenderers.register(TinkerTables.partBuilderTile.get(), tableRenderer);
  }

  static void setupClient() {
    MenuScreens.register(TinkerTables.craftingStationContainer.get(), CraftingStationScreen::new);
    MenuScreens.register(TinkerTables.tinkerStationContainer.get(), TinkerStationScreen::new);
    MenuScreens.register(TinkerTables.partBuilderContainer.get(), PartBuilderScreen::new);
    MenuScreens.register(TinkerTables.modifierWorktableContainer.get(), ModifierWorktableScreen::new);
    MenuScreens.register(TinkerTables.tinkerChestContainer.get(), TinkerChestScreen::new);
  }

  static void registerBlockColors() {
    // TODO(fabric): Forge's RegisterColorHandlersEvent was replaced with Fabric's ColorProviderRegistry
    ColorProviderRegistry.BLOCK.register((state, world, pos, index) -> {
      if (world != null && pos != null) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof TinkersChestBlockEntity) {
          return ((TinkersChestBlockEntity)te).getColor();
        }
      }
      return -1;
    }, TinkerTables.tinkersChest.get());
  }

  static void registerItemColors() {
    ColorProviderRegistry.ITEM.register((stack, index) -> ((DyeableLeatherItem)stack.getItem()).getColor(stack), TinkerTables.tinkersChest.asItem());
  }
}
