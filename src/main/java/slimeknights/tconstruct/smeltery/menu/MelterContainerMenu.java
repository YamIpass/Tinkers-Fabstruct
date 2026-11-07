package slimeknights.tconstruct.smeltery.menu;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.fluid.FluidTransferHelper;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.mantle.util.sync.ValidZeroDataSlot;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.inventory.TriggeringBaseContainerMenu;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.MelterBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;

@SuppressWarnings("UnstableApiUsage")
public class MelterContainerMenu extends TriggeringBaseContainerMenu<MelterBlockEntity> {
  public static final ResourceLocation TOOLTIP_FORMAT = TConstruct.getResource("melter");

  @SuppressWarnings("MismatchedReadAndWriteOfArray")
  @Getter
  private final Slot[] inputs;
  @Getter
  private boolean hasFuelSlot = false;
  public MelterContainerMenu(int id, @Nullable Inventory inv, @Nullable MelterBlockEntity melter) {
    super(TinkerSmeltery.melterContainer.get(), id, inv, melter);

    // create slots
    if (melter != null) {
      MeltingModuleInventory inventory = melter.getItemHandler();
      inputs = new Slot[inventory.getSlotCount()];
      for (int i = 0; i < inputs.length; i++) {
        inputs[i] = this.addSlot(new SmartItemHandlerSlot(inventory, i, 22, 16 + (i * 18)));
      }

      // add fuel slot if present, we only add for the melter though
      Level world = melter.getLevel();
      BlockPos down = melter.getBlockPos().below();
      if (world != null && world.getBlockState(down).is(TinkerTags.Blocks.FUEL_TANKS)) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, down, null);
        if (storage instanceof SlottedStackStorage slottedStorage) {
          this.addSlot(new SmartItemHandlerSlot(slottedStorage, 0, 151, 32));
          hasFuelSlot = true;
        }
      }

      this.addInventorySlots();

      // syncing
      Consumer<DataSlot> referenceConsumer = this::addDataSlot;
      ValidZeroDataSlot.trackIntArray(referenceConsumer, melter.getFuelModule());
      inventory.trackInts(array -> ValidZeroDataSlot.trackIntArray(referenceConsumer, array));
    } else {
      inputs = new Slot[0];
    }
  }

  public MelterContainerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
    this(id, inv, getTileEntityFromBuf(buf, MelterBlockEntity.class));
  }

  @Override
  public boolean clickMenuButton(Player player, int id) {
    if (0 <= id && id <= 3 && !player.isSpectator()) {
      ItemStack held = getCarried();
      if (!held.isEmpty()) {
        if (!player.level().isClientSide && tile != null) {
          // TODO(fabric): Forge's IFluidHandler became Fabric Storage, so the contained fluid is read from the storage view
          Storage<FluidVariant> tank = id < 2 ? tile.getTank() : tile.getFuelModule().getTank();
          TransferResult result;
          // even means drain fluid, odd means fill
          if ((id & 1) == 0) {
            result = FluidTransferHelper.fillStack(tank, held, TransferUtil.firstOrEmpty(tank));
          } else {
            result = FluidTransferHelper.interactWithStack(tank, held, TransferDirection.EMPTY_ITEM);
          }
          setCarried(FluidTransferHelper.handleUIResult(player, held, result));
        }
        return true;
      }
    }
    return false;
  }
}
