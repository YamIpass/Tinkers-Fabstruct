package slimeknights.tconstruct.shared.block;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.TinkerCommons;

public class WaxedPlatformBlock extends PlatformBlock {
  private final WeatherState age;
  public WaxedPlatformBlock(WeatherState age, Properties prop) {
    super(prop);
    this.age = age;
    // TODO(fabric): Forge's ToolActions.AXE_WAX_OFF in Block#getToolModifiedState is replaced with the use block callback
    UseBlockCallback.EVENT.register(this::getToolModifiedState);
  }

  @Override
  protected boolean verticalConnect(BlockState state) {
    return state.is(TinkerTags.Blocks.COPPER_PLATFORMS);
  }

  public InteractionResult getToolModifiedState(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
    BlockState state = world.getBlockState(hitResult.getBlockPos());
    if (player.getItemInHand(hand).is(ItemTags.AXES) && state.is(this)) {
      if (!world.isClientSide) {
        world.setBlockAndUpdate(hitResult.getBlockPos(), TinkerCommons.copperPlatform.get(age).withPropertiesOf(state));
        world.playSound(player, hitResult.getBlockPos(), SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
        world.levelEvent(player, LevelEvent.PARTICLES_WAX_OFF, hitResult.getBlockPos(), 0);
      }
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }
}
