package slimeknights.tconstruct.smeltery.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.library.fluid.MillibucketStorageBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.MillibucketStorage;
import slimeknights.tconstruct.library.fluid.IFluidTankUpdater;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.tank.ProxyItemTank;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/** Block entity with a tank that proxies to the nested item handler */
public class ProxyTankBlockEntity extends MantleBlockEntity implements IFluidTankUpdater, SidedStorageBlockEntity, MillibucketStorageBlockEntity {
  /** Direct access to the fluid handler and item handler */
  @Getter
  private final ProxyItemTank<ProxyTankBlockEntity> itemTank = new ProxyItemTank<>(this);
  /** Last comparator strength to reduce block updates */
  private int lastStrength = -1;
  protected ProxyTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public ProxyTankBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.proxyTank.get(), pos, state);
  }


  /* Capability */
  // TODO(fabric): Forge exposed both capabilities through IFluidHandler/IItemHandler on the tile; Fabric looks up
  //  SidedStorageBlockEntity instead, and ProxyItemTank doubles as both the fluid and item storage.

  @Override
  public Storage<FluidVariant> getMillibucketStorage(@Nullable Direction direction) {
    return itemTank.getFluidStorage();
  }

  @Override
  public Storage<ItemVariant> getItemStorage(@Nullable Direction direction) {
    return itemTank;
  }


  /* Comparators */

  /**
   * Gets the current comparator strength for the tank
   * @return  Tank comparator strength
   */
  private int calculateComparatorStrength() {
    int capacity = (int) itemTank.getCapacity();
    if (capacity == 0) {
      return 0;
    }
    return 1 + (int) (14 * itemTank.getAmount() / capacity);
  }

  /** Gets the current comparator strength */
  public int getComparatorStrength() {
    if (lastStrength == -1) {
      lastStrength = calculateComparatorStrength();
    }
    return lastStrength;
  }

  @Override
  public void onTankContentsChanged() {
    if (level != null && !level.isClientSide) {
      setChangedFast();
      int newStrength = calculateComparatorStrength();
      if (newStrength != lastStrength) {
        lastStrength = newStrength;
        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
      }
    }
  }

  /* Interaction */

  /** Called when a player interacts with the fluid cannon */
  public void interact(Player player, InteractionHand hand, boolean clickedTank) {
    // skip client side
    if (level == null || level.isClientSide) {
      return;
    }

    // transfer fluid if clicked tank
    ItemStack held = player.getItemInHand(hand);
    ItemStack inventory = itemTank.getStack();
    // if we have an active tank, try interacting
    if (!inventory.isEmpty()) {
      // must have a held item to interact
      if (!held.isEmpty() && FluidTransferHelper.interactWithContainer(level, worldPosition, MillibucketStorage.of(itemTank.getFluidStorage()), player, hand).didTransfer()
        || FluidTransferHelper.interactWithFilledBucket(level, worldPosition, MillibucketStorage.of(itemTank.getFluidStorage()), player, hand, getBlockState().getValue(HORIZONTAL_FACING)).didTransfer()) {
        return;
      }
      // if we clicked the tank, don't try and swap items unless we have no tank
      // if we clicked the item, then we probably want it removed
      if (clickedTank) {
        return;
      }
    }
    // no fluid transfer? swap items around
    // inventory is empty means place item inside
    if (inventory.isEmpty()) {
      if (!held.isEmpty() && itemTank.isItemValid(0, ItemVariant.of(held))) {
        // split the stack to place into inventory
        ItemStack stack = held.split(itemTank.getSlotLimit(0));
        player.setItemInHand(hand, held.isEmpty() ? ItemStack.EMPTY : held);
        itemTank.setStack(stack);
      }
      // if not holding anything, pick up the stack
    } else if (held.isEmpty()) {
      player.setItemInHand(hand, inventory);
      itemTank.setStack(ItemStack.EMPTY);
    } else {
      // the proxy tank prefers you giving it the same instance it had before on no change
      player.addItem(inventory);
      itemTank.setStack(inventory);
    }
  }


  /* NBT */
  private static final String TAG_ITEM = "item";

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    if (tag.contains(TAG_ITEM, Tag.TAG_COMPOUND)) {
      itemTank.readFromNBT(tag.getCompound(TAG_ITEM));
    }
  }

  @Override
  protected void saveSynced(CompoundTag tag) {
    super.saveSynced(tag);
    tag.put(TAG_ITEM, itemTank.writeToNBT());
  }
}
