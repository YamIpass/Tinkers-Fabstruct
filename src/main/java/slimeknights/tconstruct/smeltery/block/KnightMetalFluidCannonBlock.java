package slimeknights.tconstruct.smeltery.block;

import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.CombatHelper;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.shared.block.KnightMetalBlock;

/** Combination of {@link FluidCannonBlock} with {@link KnightMetalBlock} */
public class KnightMetalFluidCannonBlock extends FluidCannonBlock implements LandPathNodeTypesRegistry.StaticPathNodeTypeProvider {
  public KnightMetalFluidCannonBlock(Properties properties, int capacity, float power, float velocity, float inaccuracy) {
    super(properties, capacity, power, velocity, inaccuracy);
    // TODO(fabric): Forge's Block#getBlockPathType override is replaced with Fabric's path node type registry
    LandPathNodeTypesRegistry.register(this, this);
  }

  @Override
  public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
    return KnightMetalBlock.SHAPE;
  }

  @Nullable
  @Override
  public BlockPathTypes getPathNodeType(BlockState state, boolean neighbor) {
    return BlockPathTypes.DAMAGE_OTHER;
  }

  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    entity.hurt(CombatHelper.damageSource(level, TinkerDamageTypes.KNIGHTMETAL), KnightMetalBlock.BLOCK_DAMAGE);
  }
}
