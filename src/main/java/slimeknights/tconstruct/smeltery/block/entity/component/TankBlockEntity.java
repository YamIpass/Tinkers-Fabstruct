package slimeknights.tconstruct.smeltery.block.entity.component;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.model.ModelData;
import slimeknights.tconstruct.library.fluid.MillibucketStorageBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.MillibucketStorage;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.item.TankItem;

public class TankBlockEntity extends SmelteryComponentBlockEntity implements ITankBlockEntity, SidedStorageBlockEntity, MillibucketStorageBlockEntity {
  /** Max capacity for the tank */
  // TODO(fabric): Forge's FluidType.BUCKET_VOLUME (1000 mB) has no Fabric equivalent, our MB scale uses 1000
  public static final int DEFAULT_CAPACITY = 4 * 1000;

  /**
   * Gets the capacity for the given block
   * @param block  block
   * @return  Capacity
   */
  public static long getCapacity(Block block) {
    if (block instanceof ITankBlock) {
      return ((ITankBlock) block).getCapacity();
    }
    return DEFAULT_CAPACITY;
  }

  /**
   * Gets the capacity for the given item
   * @param item  item
   * @return  Capacity
   */
  public static long getCapacity(Item item) {
    if (item instanceof BlockItem) {
      return getCapacity(((BlockItem)item).getBlock());
    }
    return DEFAULT_CAPACITY;
  }

  /** Internal fluid tank instance */
  @Getter
  protected final FluidTankAnimated tank;
  /** Last comparator strength to reduce block updates */
  @Getter @Setter
  private int lastStrength = -1;

  public TankBlockEntity(BlockPos pos, BlockState state) {
    this(pos, state, state.getBlock() instanceof ITankBlock tank
                     ? tank
                     : TinkerSmeltery.searedTank.get(TankType.FUEL_TANK));
  }

  /** Main constructor */
  public TankBlockEntity(BlockPos pos, BlockState state, ITankBlock block) {
    this(TinkerSmeltery.tank.get(), pos, state, block);
  }

  /** Extendable constructor */
  @SuppressWarnings("WeakerAccess")
  protected TankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ITankBlock block) {
    super(type, pos, state);
    tank = new FluidTankAnimated(block.getCapacity(), this);
  }


  /*
   * Tank methods
   */

  @Override
  @Nonnull
  public Storage<FluidVariant> getMillibucketStorage(@Nullable Direction direction) {
    return tank;
  }

  // TODO(fabric): Forge's getModelData()/requestModelDataUpdate() have no Fabric equivalent; data is exposed
  //  through Fabric's RenderDataBlockEntity#getRenderData() and rebuilt on access
  @Nonnull
  @Override
  public Object getRenderData() {
    return ModelData.builder()
                    .with(ModelProperties.FLUID_STACK, tank.getFluid())
                    .with(ModelProperties.TANK_CAPACITY, (int) tank.getCapacity()).build();
  }

  // TODO(fabric): 3.12.1 updated a LIGHT blockstate property on fluid change; Porting Lib's
  //  LightEmissiveBlock (SearedTankBlock#getLightEmission) computes the light dynamically instead.

  @Override
  public void onTankContentsChanged() {
    ITankBlockEntity.super.onTankContentsChanged();
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (level != null && !level.isClientSide) {
      BlockPos masterPos = getMasterPos();
      if (masterPos != null && level.getBlockEntity(masterPos) instanceof IMasterLogic master) {
        master.onServantLoad(this);
      }
    }
  }

  /*
   * NBT
   */

  /**
   * Updates the tank from an NBT tag, used in the block
   * @param nbt  tank NBT
   */
  public void updateTank(CompoundTag nbt) {
    if (nbt.isEmpty()) {
      tank.setFluid(FluidStack.EMPTY);
    } else {
      tank.readFromNBT(nbt);
    }
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  public void load(CompoundTag tag) {
    tank.setCapacity(getCapacity(getBlockState().getBlock()));
    updateTank(tag.getCompound(NBTTags.TANK));
    super.load(tag);
  }

  @Override
  public void saveSynced(CompoundTag tag) {
    super.saveSynced(tag);
    // want tank on the client on world load
    if (!tank.isEmpty()) {
      tag.put(NBTTags.TANK, tank.writeToNBT(new CompoundTag()));
    }
  }

  /** Interface for blocks to return their capacity */
  public interface ITankBlock {
    /** Gets the capacity for this tank */
    long getCapacity();
  }
}
