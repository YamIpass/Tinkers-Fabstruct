package slimeknights.tconstruct.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.world.TinkerHeadType;

@Mixin(SkullBlock.class)
public class SkullBlockMixins extends AbstractSkullBlock {

  @Shadow
  @Final
  protected static VoxelShape PIGLIN_SHAPE;

  public SkullBlockMixins(SkullBlock.Type type, Properties properties) {
    super(type, properties);
  }

  @Inject(method = "getShape", at = @At("TAIL"), cancellable = true)
  private void getCustomShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext, CallbackInfoReturnable<VoxelShape> cir) {
    if (this.getType() == TinkerHeadType.PIGLIN_BRUTE || this.getType() == TinkerHeadType.ZOMBIFIED_PIGLIN) {
      cir.setReturnValue(PIGLIN_SHAPE);
    }
  }
}
