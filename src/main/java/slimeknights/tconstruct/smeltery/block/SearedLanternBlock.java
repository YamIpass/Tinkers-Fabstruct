package slimeknights.tconstruct.smeltery.block;

import io.github.fabricators_of_create.porting_lib.block.LightEmissiveBlock;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import javax.annotation.Nullable;
import lombok.Getter;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.HitResult;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.LanternBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity.ITankBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

@SuppressWarnings("removal")
public class SearedLanternBlock extends LanternBlock implements ITankBlock, EntityBlock, BlockPickInteractionAware, LightEmissiveBlock {
  @Getter
  private final long capacity;
  public SearedLanternBlock(Properties properties, int capacity) {
    super(properties);
    this.capacity = capacity;
  }

  @Override
  protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new LanternBlockEntity(pos, state, this);
  }

  @Override
  public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
    BlockEntity te = world.getBlockEntity(pos);
    if (te instanceof TankBlockEntity tank) {
      FluidStack fluid = tank.getTank().getFluid();
      return FluidVariantAttributes.getLuminance(fluid.getType());
    }
    return state.getLightEmission();
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    CompoundTag nbt = stack.getTag();
    if (nbt != null && world.getBlockEntity(pos) instanceof TankBlockEntity tank) {
      tank.updateTank(nbt.getCompound(NBTTags.TANK));
    }
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
    return ITankBlockEntity.getComparatorInputOverride(worldIn, pos);
  }

  @Override
  public ItemStack getPickedStack(BlockState state, BlockGetter world, BlockPos pos, @org.jetbrains.annotations.Nullable Player player, @org.jetbrains.annotations.Nullable HitResult result) {
    ItemStack stack = new ItemStack(this);
    BlockEntityHelper.get(TankBlockEntity.class, world, pos).ifPresent(te -> te.setTankTag(stack));
    return stack;
  }
}
